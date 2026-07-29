package me.noynto.eosa.application;

import me.noynto.eosa.cart.Cart;
import me.noynto.eosa.cart.CartItem;
import me.noynto.eosa.cart.CartProvider;
import me.noynto.eosa.cart.CartShippingRuleProvider;
import me.noynto.eosa.jewel.JewelProvider;
import me.noynto.eosa.shared.CartId;
import me.noynto.eosa.shared.JewelId;

import java.util.ArrayList;
import java.util.List;

public record AddJewelToCart(
        CartProvider cartProvider,
        JewelProvider jewelProvider,
        CartShippingRuleProvider shippingRuleProvider
) {

    public Cart handle(Command command) {
        if (command.cartId == null || command.cartId.value() == null) {
            throw new RuntimeException("L'identifiant du panier est nécessaire.");
        }
        if (command.jewelId == null || command.jewelId.value() == null) {
            throw new RuntimeException("L'identifiant du produit est nécessaire.");
        }

        Cart cart = cartProvider.read(command.cartId)
                .orElseThrow(() -> new RuntimeException("Le panier " + command.cartId.value() + " n'existe pas."));

        var jewel = jewelProvider.read(command.jewelId)
                .orElseThrow(() -> new RuntimeException("Le produit " + command.jewelId.value() + " n'existe pas."));

        List<CartItem> items = new ArrayList<>(cart.getItems());

        var existing = items.stream()
                .filter(i -> i.jewelId().equals(command.jewelId))
                .findFirst();

        if (existing.isPresent()) {
            items.remove(existing.get());
            items.add(new CartItem(
                    existing.get().jewelId(),
                    existing.get().name(),
                    existing.get().price(),
                    existing.get().imageId(),
                    existing.get().quantity() + 1
            ));
        } else {
            var imageId = jewel.getImageIds().isEmpty() ? null : jewel.getImageIds().getFirst();
            items.add(new CartItem(command.jewelId, jewel.getName(), jewel.getPrice(), imageId, 1));
        }

        cart.setItems(items);
        cart.applyShippingRule(shippingRuleProvider.get());
        return cartProvider.write(cart);
    }

    public record Command(
            CartId cartId,
            JewelId jewelId
    ) {
    }

}