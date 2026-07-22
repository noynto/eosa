package me.noynto.eosa.infrastructure.persistence.mongo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class MongoConfiguredCharms {

    private static final String CHARMS = "charms";

    public static MongoCollection<Document> getCollection(
            MongoDatabase database
    ) {
        database.createCollection(CHARMS);
        return database.getCollection(CHARMS);
    }

}
