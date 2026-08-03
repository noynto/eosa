package me.noynto.eosa.task;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public record MigrateProductsCollectionToJewelsTask(
        MongoDatabase database
) {
    private static final String ENABLE = "EOSA_MIGRATE_PRODUCTS_TO_JEWELS_TASK";
    private static final String PRODUCTS = "products";
    private static final String JEWELS = "jewels";
    private static final Logger LOGGER = Logger.getLogger(MigrateProductsCollectionToJewelsTask.class.getName());

    public boolean task() {
        LOGGER.log(Level.INFO, "Démarrage de la migration de la collection products vers jewels.");
        try {
            MongoCollection<Document> jewels = database.getCollection(JEWELS);
            if (jewels.countDocuments() > 0) {
                LOGGER.log(Level.INFO, "La collection jewels contient déjà des documents, aucune action nécessaire.");
                return true;
            }
            MongoCollection<Document> products = database.getCollection(PRODUCTS);
            List<Document> documents = products.find().into(new ArrayList<>());
            if (documents.isEmpty()) {
                LOGGER.log(Level.INFO, "La collection products est vide ou absente, aucune action nécessaire.");
                return true;
            }
            jewels.insertMany(documents);
            LOGGER.log(Level.INFO, documents.size() + " bijoux migrés de products vers jewels. La collection products est conservée telle quelle par sécurité.");
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "La migration de products vers jewels a échoué.", e);
            return false;
        }
    }

    public static boolean activate() {
        String enable = System.getenv().getOrDefault(ENABLE, "false");
        return Boolean.parseBoolean(enable);
    }
}
