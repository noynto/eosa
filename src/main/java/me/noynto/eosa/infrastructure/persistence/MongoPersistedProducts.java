package me.noynto.eosa.infrastructure.persistence;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.InsertOneResult;
import me.noynto.eosa.product.Product;
import me.noynto.eosa.product.ProductProvider;
import me.noynto.eosa.product.ProductState;
import me.noynto.eosa.product.Variant;
import me.noynto.eosa.product.VariantState;
import me.noynto.eosa.shared.ImageId;
import me.noynto.eosa.shared.OptionId;
import me.noynto.eosa.shared.OptionValueId;
import me.noynto.eosa.shared.ProductId;
import me.noynto.eosa.shared.VariantId;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public record MongoPersistedProducts(
        MongoCollection<Document> products
) implements ProductProvider {

    public static final String ID = "_id";
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String STATE = "state";
    public static final String OPTION_IDS = "optionIds";
    public static final String DEFAULT_VARIANT_ID = "defaultVariantId";
    public static final String VARIANTS = "variants";
    public static final String VARIANT_ID = "_id";
    public static final String VARIANT_OPTION_VALUES = "optionValues";
    public static final String VARIANT_PRICE = "price";
    public static final String VARIANT_STATE = "state";
    public static final String VARIANT_STOCK = "stock";
    public static final String VARIANT_IMAGE_IDS = "imageIds";

    @Override
    public Stream<ProductId> readIds(Search search) {
        Bson projection = Projections.include(ID);
        List<Bson> filters = new ArrayList<>();
        if (search.states() != null && !search.states().isEmpty()) {
            filters.add(Filters.in(STATE, search.states().stream().map(Enum::name).toList()));
        }
        if (search.optionId() != null && search.optionValueId() != null) {
            filters.add(Filters.eq(
                    VARIANTS + "." + VARIANT_OPTION_VALUES + "." + search.optionId().value(),
                    search.optionValueId().value()
            ));
        }
        Bson filter = filters.isEmpty() ? new Document() : Filters.and(filters);
        return StreamSupport.stream(
                        products.find(filter).sort(Sorts.descending(ID)).projection(projection).spliterator(),
                        false
                )
                .map(document -> new ProductId(document.getObjectId(ID).toString()));
    }

    @Override
    public Optional<Product> read(ProductId productId) {
        ObjectId objectId;
        try {
            objectId = new ObjectId(productId.value());
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        Document result = products.find(Filters.eq(objectId)).first();
        if (result == null) {
            return Optional.empty();
        }
        return Optional.of(toProduct(result));
    }

    @Override
    public Product write(Product product) {
        List<Document> variantDocuments = product.getVariants().stream()
                .map(this::toDocument)
                .toList();
        List<String> optionIds = product.getOptionIds().stream().map(OptionId::value).toList();
        String defaultVariantId = product.getDefaultVariantId() == null ? null : product.getDefaultVariantId().value();

        if (product.getId() == null) {
            Document newDocument = new Document()
                    .append(NAME, product.getName())
                    .append(STATE, product.getState() == null ? null : product.getState().name());
            InsertOneResult result = products.insertOne(newDocument);
            BsonValue generatedId = result.getInsertedId();
            if (generatedId == null) {
                throw new IllegalStateException("Le produit enregistré n'a pas généré d'identifiant.");
            }
            product.setId(new ProductId(generatedId.asObjectId().getValue().toString()));
        } else {
            products.updateOne(
                    Filters.eq(new ObjectId(product.getId().value())),
                    Updates.combine(
                            Updates.set(NAME, product.getName()),
                            Updates.set(DESCRIPTION, product.getDescription()),
                            Updates.set(STATE, product.getState() == null ? null : product.getState().name()),
                            Updates.set(OPTION_IDS, optionIds),
                            Updates.set(DEFAULT_VARIANT_ID, defaultVariantId),
                            Updates.set(VARIANTS, variantDocuments)
                    )
            );
        }
        return product;
    }

    private Product toProduct(Document document) {
        Product product = new Product();
        product.setId(new ProductId(document.get(ID, ObjectId.class).toString()));
        product.setName(document.get(NAME, String.class));
        product.setDescription(document.get(DESCRIPTION, String.class));
        product.setState(document.get(STATE, String.class) == null ? null : ProductState.valueOf(document.get(STATE, String.class)));
        List<String> rawOptionIds = document.getList(OPTION_IDS, String.class);
        if (rawOptionIds != null) {
            product.setOptionIds(rawOptionIds.stream().map(OptionId::new).toList());
        }
        String rawDefaultVariantId = document.get(DEFAULT_VARIANT_ID, String.class);
        product.setDefaultVariantId(rawDefaultVariantId == null ? null : new VariantId(rawDefaultVariantId));
        List<Document> rawVariants = document.getList(VARIANTS, Document.class);
        if (rawVariants != null) {
            product.setVariants(rawVariants.stream().map(this::toVariant).toList());
        }
        return product;
    }

    private Variant toVariant(Document document) {
        Variant variant = new Variant();
        variant.setId(new VariantId(document.get(VARIANT_ID, String.class)));
        Document rawOptionValues = document.get(VARIANT_OPTION_VALUES, Document.class);
        if (rawOptionValues != null) {
            Map<OptionId, OptionValueId> optionValues = new LinkedHashMap<>();
            for (String key : rawOptionValues.keySet()) {
                optionValues.put(new OptionId(key), new OptionValueId(rawOptionValues.getString(key)));
            }
            variant.setOptionValues(optionValues);
        }
        Decimal128 rawPrice = document.get(VARIANT_PRICE, Decimal128.class);
        variant.setPrice(rawPrice == null ? null : rawPrice.bigDecimalValue());
        String rawState = document.get(VARIANT_STATE, String.class);
        variant.setState(rawState == null ? null : VariantState.valueOf(rawState));
        Integer rawStock = document.get(VARIANT_STOCK, Integer.class);
        variant.setStock(rawStock == null ? 0 : rawStock);
        List<String> rawImageIds = document.getList(VARIANT_IMAGE_IDS, String.class);
        if (rawImageIds != null) {
            variant.setImageIds(rawImageIds.stream().map(ImageId::new).toList());
        }
        return variant;
    }

    private Document toDocument(Variant variant) {
        Document optionValues = new Document();
        variant.getOptionValues().forEach((optionId, optionValueId) ->
                optionValues.append(optionId.value(), optionValueId.value()));
        return new Document()
                .append(VARIANT_ID, variant.getId().value())
                .append(VARIANT_OPTION_VALUES, optionValues)
                .append(VARIANT_PRICE, variant.getPrice())
                .append(VARIANT_STATE, variant.getState() == null ? null : variant.getState().name())
                .append(VARIANT_STOCK, variant.getStock())
                .append(VARIANT_IMAGE_IDS, variant.getImageIds().stream().map(ImageId::value).toList());
    }

}
