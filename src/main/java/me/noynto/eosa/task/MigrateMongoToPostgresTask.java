package me.noynto.eosa.task;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.Document;
import org.bson.types.Decimal128;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public record MigrateMongoToPostgresTask(
        MongoDatabase mongoDatabase,
        DataSource dataSource
) {
    private static final String ENABLE = "EOSA_MIGRATE_MONGO_TO_POSTGRES_TASK";
    private static final Logger LOGGER = Logger.getLogger(MigrateMongoToPostgresTask.class.getName());

    public boolean task() {
        LOGGER.log(Level.INFO, "Démarrage de la migration MongoDB vers PostgreSQL.");
        try (Connection connection = dataSource.getConnection()) {
            if (alreadyMigrated(connection)) {
                LOGGER.log(Level.INFO, "Les tables PostgreSQL contiennent déjà des données, aucune action nécessaire.");
                return true;
            }
            connection.setAutoCommit(false);
            try {
                Map<String, UUID> imageIds = migrateImages(connection);
                Map<String, UUID> identityIds = migrateIdentities(connection);
                migrateIdentitySessions(connection, identityIds);
                Map<String, UUID> jewelIds = migrateJewels(connection, imageIds);
                migrateCarts(connection, jewelIds, imageIds);
                connection.commit();
                LOGGER.log(Level.INFO, "Migration MongoDB vers PostgreSQL terminée avec succès.");
                return true;
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "La migration MongoDB vers PostgreSQL a échoué.", e);
            return false;
        }
    }

    private boolean alreadyMigrated(Connection connection) throws SQLException {
        for (String table : List.of("identities", "jewels", "carts", "images")) {
            try (PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM " + table + " LIMIT 1");
                 ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) return true;
            }
        }
        return false;
    }

    private Map<String, UUID> migrateImages(Connection connection) throws SQLException {
        Map<String, UUID> ids = new HashMap<>();
        GridFSBucket bucket = GridFSBuckets.create(mongoDatabase, "images");
        String sql = "INSERT INTO images (id, name, format, content) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (GridFSFile file : bucket.find()) {
                byte[] content;
                try (InputStream in = bucket.openDownloadStream(file.getObjectId())) {
                    content = in.readAllBytes();
                } catch (IOException e) {
                    throw new UncheckedIOException("Impossible de lire l'image " + file.getObjectId() + ".", e);
                }
                UUID newId = UUID.randomUUID();
                String format = file.getMetadata() != null ? file.getMetadata().getString("format") : null;
                statement.setObject(1, newId);
                statement.setString(2, file.getFilename());
                statement.setString(3, format);
                statement.setBytes(4, content);
                statement.executeUpdate();
                ids.put(file.getObjectId().toString(), newId);
            }
        }
        LOGGER.log(Level.INFO, ids.size() + " images migrées.");
        return ids;
    }

    private Map<String, UUID> migrateIdentities(Connection connection) throws SQLException {
        Map<String, UUID> ids = new HashMap<>();
        MongoCollection<Document> identities = mongoDatabase.getCollection("identities");
        String sql = "INSERT INTO identities (id, name, secret, administrator) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (Document doc : identities.find()) {
                UUID newId = UUID.randomUUID();
                Boolean administrator = doc.getBoolean("administrator");
                statement.setObject(1, newId);
                statement.setString(2, doc.getString("name"));
                statement.setString(3, doc.getString("secret"));
                statement.setBoolean(4, administrator != null && administrator);
                statement.executeUpdate();
                ids.put(doc.getObjectId("_id").toString(), newId);
            }
        }
        LOGGER.log(Level.INFO, ids.size() + " identités migrées.");
        return ids;
    }

    private void migrateIdentitySessions(Connection connection, Map<String, UUID> identityIds) throws SQLException {
        MongoCollection<Document> sessions = mongoDatabase.getCollection("identity_sessions");
        String sql = "INSERT INTO identity_sessions (id, identity_id, begin_at) VALUES (?, ?, ?)";
        int count = 0;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (Document doc : sessions.find()) {
                String oldIdentityId = doc.getString("identityId");
                UUID mappedIdentityId = oldIdentityId != null ? identityIds.get(oldIdentityId) : null;
                Date begin = doc.getDate("begin");
                statement.setObject(1, UUID.randomUUID());
                statement.setObject(2, mappedIdentityId);
                statement.setObject(3, begin != null ? begin.toInstant().atOffset(ZoneOffset.UTC) : null);
                statement.executeUpdate();
                count++;
            }
        }
        LOGGER.log(Level.INFO, count + " sessions d'identité migrées.");
    }

    private Map<String, UUID> migrateJewels(Connection connection, Map<String, UUID> imageIds) throws SQLException {
        Map<String, UUID> ids = new HashMap<>();
        MongoCollection<Document> jewels = mongoDatabase.getCollection("jewels");
        String insertJewel = "INSERT INTO jewels (id, name, tagline, price, state, category) VALUES (?, ?, ?, ?, ?, ?)";
        String insertImage = "INSERT INTO jewel_images (jewel_id, image_id, position) VALUES (?, ?, ?)";
        try (PreparedStatement jewelStatement = connection.prepareStatement(insertJewel);
             PreparedStatement imageStatement = connection.prepareStatement(insertImage)) {
            for (Document doc : jewels.find()) {
                UUID newId = UUID.randomUUID();
                Decimal128 price = doc.get("price", Decimal128.class);
                jewelStatement.setObject(1, newId);
                jewelStatement.setString(2, doc.getString("name"));
                jewelStatement.setString(3, doc.getString("tagline"));
                jewelStatement.setBigDecimal(4, price != null ? price.bigDecimalValue() : null);
                jewelStatement.setString(5, doc.getString("state"));
                jewelStatement.setString(6, doc.getString("category"));
                jewelStatement.executeUpdate();
                ids.put(doc.getObjectId("_id").toString(), newId);

                List<String> rawImageIds = doc.getList("imageIds", String.class);
                if (rawImageIds != null) {
                    int position = 0;
                    for (String oldImageId : rawImageIds) {
                        UUID mappedImageId = imageIds.get(oldImageId);
                        if (mappedImageId == null) {
                            LOGGER.log(Level.WARNING, "Image " + oldImageId + " introuvable pour le bijou " + doc.getObjectId("_id") + ", ignorée.");
                            continue;
                        }
                        imageStatement.setObject(1, newId);
                        imageStatement.setObject(2, mappedImageId);
                        imageStatement.setInt(3, position++);
                        imageStatement.executeUpdate();
                    }
                }
            }
        }
        LOGGER.log(Level.INFO, ids.size() + " bijoux migrés.");
        return ids;
    }

    private void migrateCarts(Connection connection, Map<String, UUID> jewelIds, Map<String, UUID> imageIds) throws SQLException {
        MongoCollection<Document> carts = mongoDatabase.getCollection("carts");
        String insertCart = "INSERT INTO carts (id) VALUES (?)";
        String insertItem = "INSERT INTO cart_items (cart_id, position, jewel_id, name, price, image_id, quantity) VALUES (?, ?, ?, ?, ?, ?, ?)";
        int cartCount = 0;
        try (PreparedStatement cartStatement = connection.prepareStatement(insertCart);
             PreparedStatement itemStatement = connection.prepareStatement(insertItem)) {
            for (Document doc : carts.find()) {
                UUID newId = UUID.randomUUID();
                cartStatement.setObject(1, newId);
                cartStatement.executeUpdate();
                cartCount++;

                List<Document> rawItems = doc.getList("items", Document.class);
                if (rawItems != null) {
                    int position = 0;
                    for (Document item : rawItems) {
                        String oldJewelId = item.getString("jewelId");
                        UUID mappedJewelId = oldJewelId != null ? jewelIds.get(oldJewelId) : null;
                        if (mappedJewelId == null) {
                            LOGGER.log(Level.WARNING, "Bijou " + oldJewelId + " introuvable pour un item du panier " + doc.getObjectId("_id") + ", item ignoré.");
                            continue;
                        }
                        String oldImageId = item.getString("imageId");
                        UUID mappedImageId = oldImageId != null ? imageIds.get(oldImageId) : null;
                        String price = item.getString("price");

                        itemStatement.setObject(1, newId);
                        itemStatement.setInt(2, position++);
                        itemStatement.setObject(3, mappedJewelId);
                        itemStatement.setString(4, item.getString("name"));
                        itemStatement.setBigDecimal(5, price != null ? new BigDecimal(price) : null);
                        itemStatement.setObject(6, mappedImageId);
                        itemStatement.setObject(7, item.getInteger("quantity"));
                        itemStatement.executeUpdate();
                    }
                }
            }
        }
        LOGGER.log(Level.INFO, cartCount + " paniers migrés.");
    }

    public static boolean activate() {
        String enable = System.getenv().getOrDefault(ENABLE, "false");
        return Boolean.parseBoolean(enable);
    }
}
