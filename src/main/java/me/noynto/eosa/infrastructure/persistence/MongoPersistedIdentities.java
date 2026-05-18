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
    public Stream<IdentityId> readIds() {
        Bson projection = Projections.include(ID);
        return StreamSupport.stream(
                        identities.find().projection(projection).spliterator(),
                        false
                )
                .map(document -> document.getObjectId(ID).toString())
                .map(IdentityId::new);
    }

    @Override
    public Identity read(IdentityId identityId) throws UnknownIdentity {
        Document result = identities.find(Filters.eq(identityId.value())).first();
        if (result == null) {
            throw new UnknownIdentity(identityId);
        }
        Identity identity = new Identity();
        identity.setId(new IdentityId(result.get(ID, ObjectId.class).toString()));
        identity.setName(result.get(NAME, String.class));
        identity.setSecret(result.get(SECRET, String.class));
        identity.setAdministrator(result.getBoolean(ADMINISTRATOR));
        return identity;
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
            identity.setId(new IdentityId(result.getInsertedId().asObjectId().toString()));
        } else {
            UpdateResult result = identities.updateOne(
                    Filters.eq(identity.getId().value()),
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
