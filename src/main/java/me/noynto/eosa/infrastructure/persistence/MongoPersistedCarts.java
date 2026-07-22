package me.noynto.eosa.infrastructure.persistence;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.InsertOneResult;
import me.noynto.eosa.cart.Cart;
import me.noynto.eosa.cart.CartItem;
import me.noynto.eosa.cart.CartProvider;
import me.noynto.eosa.shared.CartId;
import me.noynto.eosa.shared.CharmId;
import me.noynto.eosa.shared.ImageId;
import me.noynto.eosa.shared.VariantId;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public record MongoPersistedCarts(
        MongoCollection<Document> carts
) implements CartProvider {

    private static final String ID = "_id";
    private static final String ITEMS = "items";
    private static final String VARIANT_ID = "variantId";
    private static final String CHARM_ID = "charmId";
    private static final String NAME = "name";
    private static final String PRICE = "price";
    private static final String CHARM_ADDITIONAL_PRICE = "charmAdditionalPrice";
    private static final String IMAGE_ID = "imageId";
    private static final String QUANTITY = "quantity";

    @Override
    public Optional<Cart> read(CartId cartId) {
        ObjectId objectId;
        try {
            objectId = new ObjectId(cartId.value());
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        Document result = carts.find(Filters.eq(objectId)).first();
        if (result == null) {
            return Optional.empty();
        }
        Cart cart = new Cart();
        cart.setId(new CartId(result.getObjectId(ID).toString()));
        List<Document> rawItems = result.getList(ITEMS, Document.class);
        if (rawItems != null) {
            cart.setItems(rawItems.stream().map(doc -> new CartItem(
                    new VariantId(doc.getString(VARIANT_ID)),
                    doc.getString(CHARM_ID) != null ? new CharmId(doc.getString(CHARM_ID)) : null,
                    doc.getString(NAME),
                    new BigDecimal(doc.getString(PRICE)),
                    doc.getString(CHARM_ADDITIONAL_PRICE) != null ? new BigDecimal(doc.getString(CHARM_ADDITIONAL_PRICE)) : null,
                    doc.getString(IMAGE_ID) != null ? new ImageId(doc.getString(IMAGE_ID)) : null,
                    doc.getInteger(QUANTITY)
            )).toList());
        }
        return Optional.of(cart);
    }

    @Override
    public Cart write(Cart cart) {
        List<Document> itemDocs = cart.getItems().stream().map(item -> new Document()
                .append(VARIANT_ID, item.variantId().value())
                .append(CHARM_ID, item.charmId() != null ? item.charmId().value() : null)
                .append(NAME, item.name())
                .append(PRICE, item.price().toPlainString())
                .append(CHARM_ADDITIONAL_PRICE, item.charmAdditionalPrice() != null ? item.charmAdditionalPrice().toPlainString() : null)
                .append(IMAGE_ID, item.imageId() != null ? item.imageId().value() : null)
                .append(QUANTITY, item.quantity())
        ).toList();

        if (cart.getId() == null) {
            Document newDocument = new Document().append(ITEMS, itemDocs);
            InsertOneResult result = carts.insertOne(newDocument);
            BsonValue generatedId = result.getInsertedId();
            if (generatedId == null) {
                throw new IllegalStateException("Le panier enregistré n'a pas généré d'identifiant.");
            }
            cart.setId(new CartId(generatedId.asObjectId().getValue().toString()));
        } else {
            carts.updateOne(
                    Filters.eq(new ObjectId(cart.getId().value())),
                    Updates.set(ITEMS, itemDocs)
            );
        }
        return cart;
    }

}
