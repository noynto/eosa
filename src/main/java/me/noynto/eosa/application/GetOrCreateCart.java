package me.noynto.eosa.application;

import me.noynto.eosa.cart.Cart;
import me.noynto.eosa.cart.CartProvider;
import me.noynto.eosa.cart.CartShippingRuleProvider;
import me.noynto.eosa.shared.CartId;

public record GetOrCreateCart(
        CartProvider cartProvider,
        CartShippingRuleProvider shippingRuleProvider
) {

    public Cart handle(Command command) {
        Cart cart;
        if (command.cartId != null && command.cartId.value() != null) {
            cart = cartProvider.read(command.cartId).orElseGet(() -> cartProvider.write(new Cart()));
        } else {
            cart = cartProvider.write(new Cart());
        }
        cart.applyShippingRule(shippingRuleProvider.get());
        return cart;
    }

    public record Command(
            CartId cartId
    ) {
    }

}