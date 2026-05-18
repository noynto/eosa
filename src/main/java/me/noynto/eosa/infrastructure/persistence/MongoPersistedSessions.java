package me.noynto.eosa.infrastructure.persistence;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import me.noynto.eosa.session.Session;
import me.noynto.eosa.session.SessionProvider;
import me.noynto.eosa.shared.IdentityId;
import me.noynto.eosa.shared.SessionId;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public record MongoPersistedSessions(
        MongoCollection<Document> identities
) implements SessionProvider {

    public static final String ID = "_id";
    public static final String IDENTITY_ID = "identity_id";

    @Override
    public Stream<SessionId> readIds() {
        Bson projection = Projections.include(ID);
        return StreamSupport.stream(
                        identities.find().projection(projection).spliterator(),
                        false
                )
                .map(document -> document.getObjectId(ID).toString())
                .map(SessionId::new);
    }

    @Override
    public Session read(SessionId sessionId) throws UnknownSession {
        Document result = identities.find(Filters.eq(sessionId.value())).first();
        if (result == null) {
            throw new UnknownSession(sessionId);
        }
        Session product = new Session();
        product.setId(new SessionId(result.get(ID, ObjectId.class).toString()));
        product.setIdentityId(new IdentityId(result.get(IDENTITY_ID, String.class)));
        return product;
    }

    @Override
    public Session write(Session session) {
        if (session.getId() == null) {
            Document newDocument = new Document()
                    .append(IDENTITY_ID, session.getIdentityId().value());
            InsertOneResult result = identities.insertOne(newDocument);
            BsonValue generatedId = result.getInsertedId();
            if (generatedId == null) {
                throw new IllegalStateException("La session enregistrée n'a pas généré d'identifiant.");
            }
            session.setId(new SessionId(result.getInsertedId().asObjectId().toString()));
        } else {
            UpdateResult result = identities.updateOne(
                    Filters.eq(session.getId().value()),
                    Updates.combine(
                            Updates.set(IDENTITY_ID, session.getIdentityId().value())
                    )
            );
        }
        return session;
    }

}
