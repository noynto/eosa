package me.noynto.eosa.infrastructure.persistence;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.InsertOneResult;
import me.noynto.eosa.identity.IdentitySession;
import me.noynto.eosa.identity.IdentitySessionProvider;
import me.noynto.eosa.shared.IdentityId;
import me.noynto.eosa.shared.IdentitySessionId;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public record MongoPersistedIdentitySessions(
        MongoCollection<Document> sessions
) implements IdentitySessionProvider {

    private static final String ID = "_id";
    private static final String IDENTITY_ID = "identityId";
    private static final String BEGIN = "begin";

    @Override
    public Stream<IdentitySessionId> readIds() {
        return StreamSupport.stream(
                sessions.find().projection(new Document(ID, 1)).spliterator(), false
        ).map(doc -> new IdentitySessionId(doc.getObjectId(ID).toString()));
    }

    @Override
    public Optional<IdentitySession> read(IdentitySessionId id) {
        ObjectId objectId;
        try {
            objectId = new ObjectId(id.value());
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        Document doc = sessions.find(Filters.eq(objectId)).first();
        return doc == null ? Optional.empty() : Optional.of(fromDocument(doc));
    }

    @Override
    public IdentitySession write(IdentitySession session) {
        Document doc = new Document()
                .append(IDENTITY_ID, session.getIdentityId() != null ? session.getIdentityId().value() : null)
                .append(BEGIN, session.getBegin() != null
                        ? Date.from(session.getBegin().toInstant(ZoneOffset.UTC))
                        : null);

        if (session.getId() == null) {
            InsertOneResult result = sessions.insertOne(doc);
            BsonValue generatedId = result.getInsertedId();
            if (generatedId == null) {
                throw new IllegalStateException("La session d'identité n'a pas généré d'identifiant.");
            }
            session.setId(new IdentitySessionId(generatedId.asObjectId().getValue().toString()));
        } else {
            sessions.replaceOne(Filters.eq(new ObjectId(session.getId().value())), doc);
        }
        return session;
    }

    private IdentitySession fromDocument(Document doc) {
        IdentitySession session = new IdentitySession();
        session.setId(new IdentitySessionId(doc.getObjectId(ID).toString()));
        String identityId = doc.getString(IDENTITY_ID);
        if (identityId != null) session.setIdentityId(new IdentityId(identityId));
        Date begin = doc.getDate(BEGIN);
        if (begin != null) session.setBegin(LocalDateTime.ofInstant(begin.toInstant(), ZoneOffset.UTC));
        return session;
    }

}