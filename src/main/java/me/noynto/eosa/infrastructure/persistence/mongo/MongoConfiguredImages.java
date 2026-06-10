package me.noynto.eosa.infrastructure.persistence.mongo;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;

public class MongoConfiguredImages {

    private static final String BUCKET = "images";

    public static GridFSBucket getBucket(MongoDatabase database) {
        return GridFSBuckets.create(database, BUCKET);
    }

}