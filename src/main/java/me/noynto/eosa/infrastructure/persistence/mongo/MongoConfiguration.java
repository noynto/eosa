package me.noynto.eosa.infrastructure.persistence.mongo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import java.util.Objects;

public class MongoConfiguration {
    private static final String EOSA = "eosa";

    public MongoProperties getProperties() {
        String url = Objects.requireNonNull(System.getenv("EOSA_MONGO_URL"), "L'url du serveur Mongo est obligatoir.");
        return new MongoProperties(url);
    }

    public MongoDatabase getDatabase(
            MongoProperties properties
    ) {
        MongoClient client = MongoClients.create(properties.url());
        return client.getDatabase(EOSA);
    }

}
