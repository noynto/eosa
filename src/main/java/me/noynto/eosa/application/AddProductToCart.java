package me.noynto.eosa.application;

import me.noynto.eosa.cart.Cart;
import me.noynto.eosa.cart.CartItem;
import me.noynto.eosa.cart.CartProvider;
import me.noynto.eosa.product.ProductProvider;
import me.noynto.eosa.shared.CartId;
import me.noynto.eosa.shared.ProductId;

import java.util.ArrayList;
import java.util.List;

public record
AddProductToCart(
        CartProvider cartProvider,
        ProductProvider productProvider
) {

    public Cart handle(Command command) {
        if (command.cartId == null || command.cartId.value() == null) {
            throw new RuntimeException("L'identifiant du panier est nécessaire.");
        }
        if (command.productId == null || command.productId.value() == null) {
            throw new RuntimeException("L'identifiant du produit est nécessaire.");
        }

        Cart cart = cartProvider.read(command.cartId)
                .orElseThrow(() -> new RuntimeException("Le panier " + command.cartId.value() + " n'existe pas."));

        var product = productProvider.read(command.productId)
                .orElseThrow(() -> new RuntimeException("Le produit " + command.productId.value() + " n'existe pas."));

        List<CartItem> items = new ArrayList<>(cart.getItems());

        var existing = items.stream()
                .filter(i -> i.productId().equals(command.productId))
                .findFirst();

        if (existing.isPresent()) {
            items.remove(existing.get());
            items.add(new CartItem(
                    existing.get().productId(),
                    existing.get().name(),
                    existing.get().price(),
                    existing.get().imageId(),
                    existing.get().quantity() + 1
            ));
        } else {
            var imageId = product.getImageIds().isEmpty() ? null : product.getImageIds().getFirst();
            items.add(new CartItem(command.productId, product.getName(), product.getPrice(), imageId, 1));
        }

        cart.setItems(items);
        return cartProvider.write(cart);
    }

    public record Command(
            CartId cartId,
            ProductId productId
    ) {
    }

}