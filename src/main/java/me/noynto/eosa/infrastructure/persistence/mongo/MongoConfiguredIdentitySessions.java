package me.noynto.eosa.infrastructure.persistence.mongo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class MongoConfiguredIdentitySessions {

    private static final String IDENTITY_SESSIONS = "identity_sessions";

    public static MongoCollection<Document> getCollection(MongoDatabase database) {
        database.createCollection(IDENTITY_SESSIONS);
        return database.getCollection(IDENTITY_SESSIONS);
    }

}