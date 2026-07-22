package me.noynto.eosa.infrastructure.persistence;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.InsertOneResult;
import me.noynto.eosa.option.Option;
import me.noynto.eosa.option.OptionProvider;
import me.noynto.eosa.option.OptionValue;
import me.noynto.eosa.shared.OptionId;
import me.noynto.eosa.shared.OptionValueId;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public record MongoPersistedOptions(
        MongoCollection<Document> options
) implements OptionProvider {

    public static final String ID = "_id";
    public static final String NAME = "name";
    public static final String INTRO_TEXT = "introText";
    public static final String VALUES = "values";
    public static final String VALUE_ID = "_id";
    public static final String VALUE_LABEL = "label";
    public static final String VALUE_DESCRIPTION = "description";

    @Override
    public Stream<OptionId> readIds() {
        Bson projection = Projections.include(ID);
        return StreamSupport.stream(
                        options.find().sort(Sorts.descending(ID)).projection(projection).spliterator(),
                        false
                )
                .map(document -> new OptionId(document.getObjectId(ID).toString()));
    }

    @Override
    public Optional<Option> read(OptionId optionId) {
        ObjectId objectId;
        try {
            objectId = new ObjectId(optionId.value());
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        Document result = options.find(Filters.eq(objectId)).first();
        if (result == null) {
            return Optional.empty();
        }
        return Optional.of(toOption(result));
    }

    @Override
    public Option write(Option option) {
        List<Document> valueDocuments = option.getValues().stream()
                .map(this::toDocument)
                .toList();
        if (option.getId() == null) {
            Document newDocument = new Document()
                    .append(NAME, option.getName())
                    .append(INTRO_TEXT, option.getIntroText())
                    .append(VALUES, valueDocuments);
            InsertOneResult result = options.insertOne(newDocument);
            BsonValue generatedId = result.getInsertedId();
            if (generatedId == null) {
                throw new IllegalStateException("L'option enregistrée n'a pas généré d'identifiant.");
            }
            option.setId(new OptionId(generatedId.asObjectId().getValue().toString()));
        } else {
            options.updateOne(
                    Filters.eq(new ObjectId(option.getId().value())),
                    Updates.combine(
                            Updates.set(NAME, option.getName()),
                            Updates.set(INTRO_TEXT, option.getIntroText()),
                            Updates.set(VALUES, valueDocuments)
                    )
            );
        }
        return option;
    }

    private Option toOption(Document document) {
        Option option = new Option();
        option.setId(new OptionId(document.get(ID, ObjectId.class).toString()));
        option.setName(document.get(NAME, String.class));
        option.setIntroText(document.get(INTRO_TEXT, String.class));
        List<Document> rawValues = document.getList(VALUES, Document.class);
        if (rawValues != null) {
            option.setValues(rawValues.stream().map(this::toOptionValue).toList());
        }
        return option;
    }

    private OptionValue toOptionValue(Document document) {
        OptionValue value = new OptionValue();
        value.setId(new OptionValueId(document.get(VALUE_ID, String.class)));
        value.setLabel(document.get(VALUE_LABEL, String.class));
        value.setDescription(document.get(VALUE_DESCRIPTION, String.class));
        return value;
    }

    private Document toDocument(OptionValue value) {
        return new Document()
                .append(VALUE_ID, value.getId().value())
                .append(VALUE_LABEL, value.getLabel())
                .append(VALUE_DESCRIPTION, value.getDescription());
    }

}
