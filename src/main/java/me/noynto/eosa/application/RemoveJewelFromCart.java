package me.noynto.eosa.application;

import me.noynto.eosa.cart.Cart;
import me.noynto.eosa.cart.CartProvider;
import me.noynto.eosa.cart.CartShippingRuleProvider;
import me.noynto.eosa.shared.CartId;
import me.noynto.eosa.shared.JewelId;

import java.util.ArrayList;

public record RemoveJewelFromCart(
        CartProvider cartProvider,
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

        var items = new ArrayList<>(cart.getItems());
        items.removeIf(i -> i.jewelId().value().equals(command.jewelId.value()));
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