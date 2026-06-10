package me.noynto.eosa.application;

import me.noynto.eosa.cart.Cart;
import me.noynto.eosa.cart.CartProvider;
import me.noynto.eosa.shared.CartId;

public record GetOrCreateCart(
        CartProvider cartProvider
) {

    public Cart handle(Command command) {
        if (command.cartId != null && command.cartId.value() != null) {
            return cartProvider.read(command.cartId).orElseGet(() -> cartProvider.write(new Cart()));
        }
        return cartProvider.write(new Cart());
    }

    public record Command(
            CartId cartId
    ) {
    }

}