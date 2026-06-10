package me.noynto.eosa.infrastructure.persistence.mongo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class  MongoConfiguredProducts {

    private static final String PRODUCTS = "products";

    public static MongoCollection<Document> getCollection(
            MongoDatabase database
    ) {
        database.createCollection(PRODUCTS);
        return database.getCollection(PRODUCTS);
    }


}
