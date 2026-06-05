package me.noynto.eosa.infrastructure.persistence;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.InsertOneResult;
import me.noynto.eosa.product.Product;
import me.noynto.eosa.product.ProductCategory;
import me.noynto.eosa.product.ProductProvider;
import me.noynto.eosa.product.ProductState;
import me.noynto.eosa.shared.ImageId;
import me.noynto.eosa.shared.ProductId;
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

public record MongoPersistedProducts(
        MongoCollection<Document> products
) implements ProductProvider {

    public static final String ID = "_id";
    public static final String NAME = "name";
    public static final String TAGLINE = "tagline";
    public static final String PRICE = "price";
    public static final String STATE = "state";
    public static final String CATEGORY = "category";
    public static final String IMAGE_IDS = "imageIds";

    @Override
    public Stream<ProductId> readIds(Set<ProductState> states, Set<ProductCategory> categories) {
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
                        products.find(filter).projection(projection).spliterator(),
                        false
                )
                .map(document -> document.getObjectId(ID).toString())
                .map(ProductId::new);
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
        Product product = new Product();
        product.setId(new ProductId(result.get(ID, ObjectId.class).toString()));
        product.setName(result.get(NAME, String.class));
        product.setTagline(result.get(TAGLINE, String.class));
        product.setPrice(result.get(PRICE, Decimal128.class) == null ? null :result.get(PRICE, Decimal128.class).bigDecimalValue());
        product.setState(result.get(STATE, String.class) == null ? null : ProductState.valueOf(result.get(STATE, String.class)));
        product.setCategory(result.get(CATEGORY, String.class) == null ? null : ProductCategory.valueOf(result.get(CATEGORY, String.class)));
        List<String> rawImageIds = result.getList(IMAGE_IDS, String.class);
        if (rawImageIds != null) {
            product.setImageIds(rawImageIds.stream().map(ImageId::new).toList());
        }
        return Optional.of(product);
    }

    @Override
    public Product write(Product product) {
        if (product.getId() == null) {
            Document newDocument = new Document()
                    .append(NAME, product.getName())
                    .append(STATE, product.getState());
            InsertOneResult result = products.insertOne(newDocument);
            BsonValue generatedId = result.getInsertedId();
            if (generatedId == null) {
                throw new IllegalStateException("Le produit enregistrée n'a pas généré d'identifiant.");
            }
            product.setId(new ProductId(result.getInsertedId().asObjectId().toString()));
        } else {
            products.updateOne(
                    Filters.eq(new ObjectId(product.getId().value())),
                    Updates.combine(
                            Updates.set(NAME, product.getName()),
                            Updates.set(TAGLINE, product.getTagline()),
                            Updates.set(PRICE, product.getPrice()),
                            Updates.set(STATE, product.getState()),
                            Updates.set(CATEGORY, product.getCategory()),
                            Updates.set(IMAGE_IDS, product.getImageIds().stream().map(ImageId::value).toList())
                    )
            );
        }
        return product;
    }

}
