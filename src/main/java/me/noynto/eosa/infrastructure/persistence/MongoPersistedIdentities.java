package me.noynto.eosa.infrastructure.persistence;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import me.noynto.eosa.identity.Identity;
import me.noynto.eosa.identity.IdentityProvider;
import me.noynto.eosa.shared.IdentityId;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public record MongoPersistedIdentities(
        MongoCollection<Document> identities
) implements IdentityProvider {

    public static final String ID = "_id";
    public static final String NAME = "name";
    public static final String SECRET = "secret";
    public static final String ADMINISTRATOR = "administrator";

    @Override
    public Stream<IdentityId> readIds(Boolean isAdministrator, String name) {
        List<Bson> filters = new ArrayList<>();
        if (isAdministrator != null) filters.add(Filters.eq(ADMINISTRATOR, isAdministrator));
        if (name != null) filters.add(Filters.regex(NAME, Pattern.compile("^" + Pattern.quote(name) + "$", Pattern.CASE_INSENSITIVE)));
        Bson filter = filters.isEmpty() ? new Document() : Filters.and(filters);
        return StreamSupport.stream(
                        identities.find(filter).projection(Projections.include(ID)).spliterator(),
                        false
                )
                .map(document -> document.getObjectId(ID).toString())
                .map(IdentityId::new);
    }

    @Override
    public Optional<Identity> read(IdentityId identityId) {
        ObjectId objectId;
        try {
            objectId = new ObjectId(identityId.value());
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        Document result = identities.find(Filters.eq(objectId)).first();
        if (result == null) {
            return Optional.empty();
        }
        Identity identity = new Identity();
        identity.setId(new IdentityId(result.get(ID, ObjectId.class).toString()));
        identity.setName(result.get(NAME, String.class));
        identity.setSecret(result.get(SECRET, String.class));
        identity.setAdministrator(result.getBoolean(ADMINISTRATOR));
        return Optional.of(identity);
    }

    @Override
    public Identity write(Identity identity) {
        if (identity.getId() == null) {
            Document newDocument = new Document()
                    .append(NAME, identity.getName())
                    .append(SECRET, identity.getSecret())
                    .append(ADMINISTRATOR, identity.isAdministrator());
            InsertOneResult result = identities.insertOne(newDocument);
            BsonValue generatedId = result.getInsertedId();
            if (generatedId == null) {
                throw new IllegalStateException("L'identité enregistrée n'a pas généré d'identifiant.");
            }
            identity.setId(new IdentityId(result.getInsertedId().asObjectId().getValue().toString()));
        } else {
            UpdateResult result = identities.updateOne(
                    Filters.eq(new ObjectId(identity.getId().value())),
                    Updates.combine(
                            Updates.set(NAME, identity.getName()),
                            Updates.set(SECRET, identity.getSecret()),
                            Updates.set(ADMINISTRATOR, identity.isAdministrator())
                    )
            );
        }
        return identity;
    }
}
