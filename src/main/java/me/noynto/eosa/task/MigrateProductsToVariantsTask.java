package me.noynto.eosa.task;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import me.noynto.eosa.infrastructure.persistence.mongo.MongoConfiguration;
import me.noynto.eosa.infrastructure.persistence.mongo.MongoConfiguredOptions;
import me.noynto.eosa.infrastructure.persistence.mongo.MongoConfiguredProducts;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Migration one-shot : transforme les documents "products" à plat (name, tagline, price,
 * state, category, imageIds) vers le nouveau modèle Product (parent) / Option / Variant.
 * Idempotente : ne retraite pas les documents qui possèdent déjà un champ "variants".
 * À exécuter une fois manuellement avant le déploiement du nouveau code
 * (mvn exec:java -Dexec.mainClass=me.noynto.eosa.task.MigrateProductsToVariantsTask),
 * après sauvegarde de la collection "products" (mongodump).
 */
public record MigrateProductsToVariantsTask(
        MongoCollection<Document> products,
        MongoCollection<Document> options
) {

    private static final Logger LOGGER = Logger.getLogger(MigrateProductsToVariantsTask.class.getName());

    private static final String ID = "_id";
    private static final String NAME = "name";
    private static final String TAGLINE = "tagline";
    private static final String DESCRIPTION = "description";
    private static final String PRICE = "price";
    private static final String STATE = "state";
    private static final String CATEGORY = "category";
    private static final String IMAGE_IDS = "imageIds";
    private static final String OPTION_IDS = "optionIds";
    private static final String DEFAULT_VARIANT_ID = "defaultVariantId";
    private static final String VARIANTS = "variants";
    private static final String VARIANT_OPTION_VALUES = "optionValues";
    private static final String VARIANT_STOCK = "stock";
    private static final String VALUES = "values";
    private static final String VALUE_ID = "_id";
    private static final String VALUE_LABEL = "label";
    private static final String TYPE_OPTION_NAME = "Type";

    public void task() {
        LOGGER.log(Level.INFO, "Démarrage de la migration des produits vers le modèle Product/Option/Variant.");

        String typeOptionId = ensureTypeOption();

        long migrated = 0;
        for (Document doc : products.find(Filters.exists(VARIANTS, false))) {
            migrateProduct(doc, typeOptionId);
            migrated++;
        }

        LOGGER.log(Level.INFO, migrated + " produit(s) migré(s).");
        LOGGER.log(Level.INFO, "Fin de la migration.");
    }

    private String ensureTypeOption() {
        Document existing = options.find(Filters.eq(NAME, TYPE_OPTION_NAME)).first();
        if (existing != null) {
            return existing.getObjectId(ID).toString();
        }

        Document typeOption = new Document()
                .append(NAME, TYPE_OPTION_NAME)
                .append(VALUES, List.of(
                        new Document().append(VALUE_ID, UUID.randomUUID().toString()).append(VALUE_LABEL, "Collier"),
                        new Document().append(VALUE_ID, UUID.randomUUID().toString()).append(VALUE_LABEL, "Bracelet")
                ));
        var result = options.insertOne(typeOption);
        String optionId = result.getInsertedId().asObjectId().getValue().toString();
        LOGGER.log(Level.INFO, "Option \"Type\" créée avec l'identifiant " + optionId + ".");
        return optionId;
    }

    private void migrateProduct(Document doc, String typeOptionId) {
        String productId = doc.getObjectId(ID).toString();
        String category = doc.getString(CATEGORY);
        String typeValueId = resolveTypeValueId(typeOptionId, category);

        String variantId = UUID.randomUUID().toString();
        Document variantOptionValues = new Document();
        if (typeValueId != null) {
            variantOptionValues.append(typeOptionId, typeValueId);
        }

        Document variant = new Document()
                .append(ID, variantId)
                .append(VARIANT_OPTION_VALUES, variantOptionValues)
                .append(PRICE, doc.get(PRICE, Decimal128.class))
                .append(STATE, doc.getString(STATE))
                .append(VARIANT_STOCK, 0)
                .append(IMAGE_IDS, doc.getList(IMAGE_IDS, String.class));

        products.updateOne(
                Filters.eq(new ObjectId(productId)),
                Updates.combine(
                        Updates.set(DESCRIPTION, doc.getString(TAGLINE)),
                        Updates.set(OPTION_IDS, List.of(typeOptionId)),
                        Updates.set(VARIANTS, List.of(variant)),
                        Updates.set(DEFAULT_VARIANT_ID, variantId),
                        Updates.unset(TAGLINE),
                        Updates.unset(PRICE),
                        Updates.unset(CATEGORY)
                )
        );
    }

    private String resolveTypeValueId(String typeOptionId, String category) {
        if (category == null) {
            return null;
        }
        String label = switch (category) {
            case "NECKLACE" -> "Collier";
            case "BRACELET" -> "Bracelet";
            default -> null;
        };
        if (label == null) {
            return null;
        }
        Document option = options.find(Filters.eq(new ObjectId(typeOptionId))).first();
        if (option == null) {
            return null;
        }
        List<Document> values = option.getList(VALUES, Document.class);
        return values.stream()
                .filter(v -> label.equals(v.getString(VALUE_LABEL)))
                .map(v -> v.getString(VALUE_ID))
                .findFirst()
                .orElse(null);
    }

    public static void main(String[] args) {
        var mongoProperties = MongoConfiguration.getProperties();
        MongoDatabase database = MongoConfiguration.getDatabase(mongoProperties);
        MongoCollection<Document> products = MongoConfiguredProducts.getCollection(database);
        MongoCollection<Document> options = MongoConfiguredOptions.getCollection(database);
        new MigrateProductsToVariantsTask(products, options).task();
        Runtime.getRuntime().exit(0);
    }

}
