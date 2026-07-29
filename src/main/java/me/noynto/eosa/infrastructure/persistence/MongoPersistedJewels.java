package me.noynto.eosa.infrastructure.persistence;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.InsertOneResult;
import me.noynto.eosa.jewel.Jewel;
import me.noynto.eosa.jewel.JewelCategory;
import me.noynto.eosa.jewel.JewelProvider;
import me.noynto.eosa.jewel.JewelState;
import me.noynto.eosa.shared.ImageId;
import me.noynto.eosa.shared.JewelId;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public record MongoPersistedJewels(
        MongoCollection<Document> jewels
) implements JewelProvider {

    public static final String ID = "_id";
    public static final String NAME = "name";
    public static final String TAGLINE = "tagline";
    public static final String PRICE = "price";
    public static final String STATE = "state";
    public static final String CATEGORY = "category";
    public static final String IMAGE_IDS = "imageIds";

    @Override
    public Stream<JewelId> readIds(Set<JewelState> states, Set<JewelCategory> categories) {
        Bson projection = Projections.include(ID);
        List<Bson> filters = new java.util.ArrayList<>();
        if (states != null && !states.isEmpty()) {
            filters.add(Filters.in(STATE, states.stream().map(Enum::name).toList()));
        }
        if (categories != null && !categories.isEmpty()) {
            filters.add(Filters.in(CATEGORY, categories.stream().map(Enum::name).toList()));
        }
        Bson filter = filters.isEmpty() ? new Document() : Filters.and(filters);
        return StreamSupport.stream(
                        jewels.find(filter).sort(Sorts.descending(ID)).projection(projection).spliterator(),
                        false
                )
                .map(document -> new JewelId(document.getObjectId(ID).toString()));
    }

    @Override
    public Optional<Jewel> read(JewelId jewelId) {
        ObjectId objectId;
        try {
            objectId = new ObjectId(jewelId.value());
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        Document result = jewels.find(Filters.eq(objectId)).first();
        if (result == null) {
            return Optional.empty();
        }
        Jewel jewel = new Jewel();
        JewelId pid = new JewelId(result.get(ID, ObjectId.class).toString());
        jewel.setId(pid);
        jewel.setName(result.get(NAME, String.class));
        jewel.setTagline(result.get(TAGLINE, String.class));
        jewel.setPrice(result.get(PRICE, Decimal128.class) == null ? null : result.get(PRICE, Decimal128.class).bigDecimalValue());
        jewel.setState(result.get(STATE, String.class) == null ? null : JewelState.valueOf(result.get(STATE, String.class)));
        jewel.setCategory(result.get(CATEGORY, String.class) == null ? null : JewelCategory.valueOf(result.get(CATEGORY, String.class)));
        List<String> rawImageIds = result.getList(IMAGE_IDS, String.class);
        if (rawImageIds != null) {
            jewel.setImageIds(rawImageIds.stream().map(ImageId::new).toList());
        }
        return Optional.of(jewel);
    }

    @Override
    public Jewel write(Jewel jewel) {
        if (jewel.getId() == null) {
            Document newDocument = new Document()
                    .append(NAME, jewel.getName())
                    .append(STATE, jewel.getState());
            InsertOneResult result = jewels.insertOne(newDocument);
            BsonValue generatedId = result.getInsertedId();
            if (generatedId == null) {
                throw new IllegalStateException("Le produit enregistrée n'a pas généré d'identifiant.");
            }
            jewel.setId(new JewelId(result.getInsertedId().asObjectId().getValue().toString()));
        } else {
            jewels.updateOne(
                    Filters.eq(new ObjectId(jewel.getId().value())),
                    Updates.combine(
                            Updates.set(NAME, jewel.getName()),
                            Updates.set(TAGLINE, jewel.getTagline()),
                            Updates.set(PRICE, jewel.getPrice()),
                            Updates.set(STATE, jewel.getState()),
                            Updates.set(CATEGORY, jewel.getCategory()),
                            Updates.set(IMAGE_IDS, jewel.getImageIds().stream().map(ImageId::value).toList())
                    )
            );
        }
        return jewel;
    }

}
