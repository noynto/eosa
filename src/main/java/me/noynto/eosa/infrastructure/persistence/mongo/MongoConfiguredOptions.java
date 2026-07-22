package me.noynto.eosa.infrastructure.persistence.mongo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class MongoConfiguredOptions {

    private static final String OPTIONS = "options";

    public static MongoCollection<Document> getCollection(
            MongoDatabase database
    ) {
        database.createCollection(OPTIONS);
        return database.getCollection(OPTIONS);
    }

}
