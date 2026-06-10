package me.noynto.eosa.infrastructure.persistence.mongo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class MongoConfiguredIdentities {

    private static final String IDENTITIES = "identities";

    public static MongoCollection<Document> getCollection(
            MongoDatabase database
    ) {
        database.createCollection(IDENTITIES);
        return database.getCollection(IDENTITIES);
    }


}
