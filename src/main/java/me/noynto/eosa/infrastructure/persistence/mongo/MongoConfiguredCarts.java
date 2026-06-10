package me.noynto.eosa.infrastructure.persistence.mongo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class MongoConfiguredCarts {

    private static final String CARTS = "carts";

    public static MongoCollection<Document> getCollection(MongoDatabase database) {
        database.createCollection(CARTS);
        return database.getCollection(CARTS);
    }

}