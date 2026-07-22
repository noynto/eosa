package me.noynto.eosa.application;

import me.noynto.eosa.cart.Cart;
import me.noynto.eosa.cart.CartItem;
import me.noynto.eosa.cart.CartProvider;
import me.noynto.eosa.cart.CartShippingRuleProvider;
import me.noynto.eosa.shared.CartId;
import me.noynto.eosa.shared.VariantId;

import java.util.ArrayList;

public record UpdateCartItemQuantity(
        CartProvider cartProvider,
        CartShippingRuleProvider shippingRuleProvider
) {

    public Cart handle(Command command) {
        if (command.cartId == null || command.cartId.value() == null) {
            throw new RuntimeException("L'identifiant du panier est nécessaire.");
        }
        if (command.variantId == null || command.variantId.value() == null) {
            throw new RuntimeException("L'identifiant du variant est nécessaire.");
        }
        if (command.quantity < 0) {
            throw new RuntimeException("La quantité ne peut pas être négative.");
        }

        Cart cart = cartProvider.read(command.cartId)
                .orElseThrow(() -> new RuntimeException("Le panier " + command.cartId.value() + " n'existe pas."));

        var items = new ArrayList<>(cart.getItems());
        var existing = items.stream()
                .filter(i -> i.variantId().value().equals(command.variantId.value()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Le variant " + command.variantId.value() + " n'est pas dans le panier."));

        items.remove(existing);
        if (command.quantity > 0) {
            items.add(new CartItem(
                    existing.variantId(),
                    existing.charmId(),
                    existing.name(),
                    existing.price(),
                    existing.charmAdditionalPrice(),
                    existing.imageId(),
                    command.quantity
            ));
        }

        cart.setItems(items);
        cart.applyShippingRule(shippingRuleProvider.get());
        return cartProvider.write(cart);
    }

    public record Command(
            CartId cartId,
            VariantId variantId,
            int quantity
    ) {
    }

}
