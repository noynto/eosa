package me.noynto.eosa.infrastructure.persistence;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.InsertOneResult;
import me.noynto.eosa.charm.Charm;
import me.noynto.eosa.charm.CharmProvider;
import me.noynto.eosa.charm.CharmState;
import me.noynto.eosa.shared.CharmId;
import me.noynto.eosa.shared.ImageId;
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

public record MongoPersistedCharms(
        MongoCollection<Document> charms
) implements CharmProvider {

    public static final String ID = "_id";
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String IMAGE_ID = "imageId";
    public static final String ADDITIONAL_PRICE = "additionalPrice";
    public static final String STOCK = "stock";
    public static final String STATE = "state";

    @Override
    public Stream<CharmId> readIds(Set<CharmState> states) {
        Bson projection = Projections.include(ID);
        List<Bson> filters = new java.util.ArrayList<>();
        if (states != null && !states.isEmpty()) {
            filters.add(Filters.in(STATE, states.stream().map(Enum::name).toList()));
        }
        Bson filter = filters.isEmpty() ? new Document() : Filters.and(filters);
        return StreamSupport.stream(
                        charms.find(filter).sort(Sorts.descending(ID)).projection(projection).spliterator(),
                        false
                )
                .map(document -> new CharmId(document.getObjectId(ID).toString()));
    }

    @Override
    public Optional<Charm> read(CharmId charmId) {
        ObjectId objectId;
        try {
            objectId = new ObjectId(charmId.value());
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        Document result = charms.find(Filters.eq(objectId)).first();
        if (result == null) {
            return Optional.empty();
        }
        Charm charm = new Charm();
        charm.setId(new CharmId(result.get(ID, ObjectId.class).toString()));
        charm.setName(result.get(NAME, String.class));
        charm.setDescription(result.get(DESCRIPTION, String.class));
        String rawImageId = result.get(IMAGE_ID, String.class);
        charm.setImageId(rawImageId == null ? null : new ImageId(rawImageId));
        Decimal128 rawAdditionalPrice = result.get(ADDITIONAL_PRICE, Decimal128.class);
        charm.setAdditionalPrice(rawAdditionalPrice == null ? null : rawAdditionalPrice.bigDecimalValue());
        charm.setStock(result.get(STOCK, Integer.class) == null ? 0 : result.get(STOCK, Integer.class));
        charm.setState(result.get(STATE, String.class) == null ? null : CharmState.valueOf(result.get(STATE, String.class)));
        return Optional.of(charm);
    }

    @Override
    public Charm write(Charm charm) {
        if (charm.getId() == null) {
            Document newDocument = new Document()
                    .append(NAME, charm.getName())
                    .append(STATE, charm.getState() == null ? null : charm.getState().name());
            InsertOneResult result = charms.insertOne(newDocument);
            BsonValue generatedId = result.getInsertedId();
            if (generatedId == null) {
                throw new IllegalStateException("La breloque enregistrée n'a pas généré d'identifiant.");
            }
            charm.setId(new CharmId(generatedId.asObjectId().getValue().toString()));
        } else {
            charms.updateOne(
                    Filters.eq(new ObjectId(charm.getId().value())),
                    Updates.combine(
                            Updates.set(NAME, charm.getName()),
                            Updates.set(DESCRIPTION, charm.getDescription()),
                            Updates.set(IMAGE_ID, charm.getImageId() == null ? null : charm.getImageId().value()),
                            Updates.set(ADDITIONAL_PRICE, charm.getAdditionalPrice()),
                            Updates.set(STOCK, charm.getStock()),
                            Updates.set(STATE, charm.getState() == null ? null : charm.getState().name())
                    )
            );
        }
        return charm;
    }

}
