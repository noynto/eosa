package me.noynto.eosa.infrastructure.persistence.mongo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class MongoConfiguredSessions {

    private static final String SESSIONS = "sessions";

    public MongoCollection<Document> getCollection(
            MongoDatabase database
    ) {
        database.createCollection(SESSIONS);
        return database.getCollection(SESSIONS);
    }


}
