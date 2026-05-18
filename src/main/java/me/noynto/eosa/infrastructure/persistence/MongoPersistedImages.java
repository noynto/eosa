package me.noynto.eosa.infrastructure.persistence;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import com.mongodb.client.model.Filters;
import me.noynto.eosa.image.Image;
import me.noynto.eosa.image.ImageProvider;
import me.noynto.eosa.shared.ImageId;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.Optional;

public record MongoPersistedImages(
        GridFSBucket bucket
) implements ImageProvider {

    private static final String FORMAT = "format";

    @Override
    public Image upload(Image image) {
        GridFSUploadOptions options = new GridFSUploadOptions()
                .metadata(new Document(FORMAT, image.getFormat()));
        ObjectId fileId = bucket.uploadFromStream(image.getName(), image.getContent(), options);
        image.setId(new ImageId(fileId.toHexString()));
        return image;
    }

    @Override
    public Optional<Image> download(ImageId imageId) {
        ObjectId objectId = new ObjectId(imageId.value());
        GridFSFile file = bucket.find(Filters.eq(objectId)).first();
        if (file == null) {
            return Optional.empty();
        }
        String format = file.getMetadata() != null ? file.getMetadata().getString(FORMAT) : null;
        Image image = new Image();
        image.setId(imageId);
        image.setFormat(format);
        image.setContent(bucket.openDownloadStream(objectId));
        return Optional.of(image);
    }

}