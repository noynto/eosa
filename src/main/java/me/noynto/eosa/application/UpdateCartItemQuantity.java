package me.noynto.eosa.application;

import me.noynto.eosa.cart.Cart;
import me.noynto.eosa.cart.CartItem;
import me.noynto.eosa.cart.CartProvider;
import me.noynto.eosa.cart.CartShippingRuleProvider;
import me.noynto.eosa.shared.CartId;
import me.noynto.eosa.shared.CartItemId;

import java.util.ArrayList;

public record UpdateCartItemQuantity(
        CartProvider cartProvider,
        CartShippingRuleProvider shippingRuleProvider
) {

    public Cart handle(Command command) {
        if (command.cartId == null || command.cartId.value() == null) {
            throw new RuntimeException("L'identifiant du panier est nécessaire.");
        }
        if (command.itemId == null || command.itemId.value() == null) {
            throw new RuntimeException("L'identifiant de la ligne de panier est nécessaire.");
        }
        if (command.quantity < 0) {
            throw new RuntimeException("La quantité ne peut pas être négative.");
        }

        Cart cart = cartProvider.read(command.cartId)
                .orElseThrow(() -> new RuntimeException("Le panier " + command.cartId.value() + " n'existe pas."));

        var items = new ArrayList<>(cart.getItems());
        var existing = items.stream()
                .filter(i -> i.id().value().equals(command.itemId.value()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("La ligne " + command.itemId.value() + " n'est pas dans le panier."));

        items.remove(existing);
        if (command.quantity > 0) {
            items.add(new CartItem(
                    existing.id(),
                    existing.jewelId(),
                    existing.name(),
                    existing.price(),
                    existing.imageId(),
                    command.quantity,
                    existing.metalColorId(),
                    existing.metalColorName(),
                    existing.metalColorImageId(),
                    existing.charms()
            ));
        }

        cart.setItems(items);
        cart.applyShippingRule(shippingRuleProvider.get());
        return cartProvider.write(cart);
    }

    public record Command(
            CartId cartId,
            CartItemId itemId,
            int quantity
    ) {
    }

}
