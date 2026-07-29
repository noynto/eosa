package me.noynto.eosa.infrastructure.persistence.mongo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class  MongoConfiguredJewels {

    private static final String JEWELS = "jewels";

    public static MongoCollection<Document> getCollection(
            MongoDatabase database
    ) {
        database.createCollection(JEWELS);
        return database.getCollection(JEWELS);
    }


}
