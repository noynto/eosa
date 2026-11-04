package me.noynto.eosa.application;

import me.noynto.eosa.cart.Cart;
import me.noynto.eosa.cart.CartProvider;
import me.noynto.eosa.cart.CartShippingRuleProvider;
import me.noynto.eosa.shared.CartId;

/**
 * Lit un panier existant, avec ses frais de livraison selon la règle en vigueur.
 */
public record ReadCart(
        CartProvider cartProvider,
        CartShippingRuleProvider shippingRuleProvider
) {

    public Result handle(Command command) {
        if (command.cartId == null || command.cartId.value() == null) {
            throw new RuntimeException("L'identifiant du panier est nécessaire.");
        }
        Cart cart = cartProvider.read(command.cartId)
                .orElseThrow(() -> new RuntimeException("Le panier " + command.cartId.value() + " n'existe pas."));
        cart.applyShippingRule(shippingRuleProvider.get());
        return new Result(cart);
    }

    public record Command(
            CartId cartId
    ) {
    }

    public record Result(
            Cart cart
    ) {
    }

}
