package me.noynto.eosa.infrastructure.persistence;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.InsertOneResult;
import me.noynto.eosa.product.Product;
import me.noynto.eosa.product.ProductProvider;
import me.noynto.eosa.shared.ImageId;
import me.noynto.eosa.shared.ProductId;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public record MongoPersistedProducts(
        MongoCollection<Document> products
) implements ProductProvider {

    public static final String ID = "_id";
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String PRICE = "price";
    public static final String IMAGE_IDS = "imageIds";

    @Override
    public Stream<ProductId> readIds() {
        Bson projection = Projections.include(ID);
        return StreamSupport.stream(
                        products.find().projection(projection).spliterator(),
                        false
                )
                .map(document -> document.getObjectId(ID).toString())
                .map(ProductId::new);
    }

    @Override
    public Product read(ProductId productId) throws UnknownProduct {
        Document result = products.find(Filters.eq(new ObjectId(productId.value()))).first();
        if (result == null) {
            throw new UnknownProduct(productId);
        }
        Product product = new Product();
        product.setId(new ProductId(result.get(ID, ObjectId.class).toString()));
        product.setName(result.get(NAME, String.class));
        product.setDescription(result.get(DESCRIPTION, String.class));
        product.setPrice(result.get(PRICE, Decimal128.class).bigDecimalValue());
        List<String> rawImageIds = result.getList(IMAGE_IDS, String.class);
        if (rawImageIds != null) {
            product.setImageIds(rawImageIds.stream().map(ImageId::new).toList());
        }
        return product;
    }

    @Override
    public Product write(Product product) {
        if (product.getId() == null) {
            Document newDocument = new Document()
                    .append(NAME, product.getName())
                    .append(DESCRIPTION, product.getDescription())
                    .append(PRICE, product.getPrice());
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
                            Updates.set(DESCRIPTION, product.getDescription()),
                            Updates.set(PRICE, product.getPrice()),
                            Updates.set(IMAGE_IDS, product.getImageIds().stream().map(ImageId::value).toList())
                    )
            );
        }
        return product;
    }

}
