package me.noynto.eosa.application;

import me.noynto.eosa.cart.Cart;
import me.noynto.eosa.cart.CartProvider;
import me.noynto.eosa.shared.CartId;

/** Détache le code de promotion du panier. Sans effet s'il n'y en a pas. */
public record RemovePromoCode(
        CartProvider cartProvider
) {

    public Result handle(Command command) {
        if (command.cartId == null || command.cartId.value() == null) {
            throw new RuntimeException("L'identifiant du panier est nécessaire.");
        }
        Cart cart = cartProvider.read(command.cartId)
                .orElseThrow(() -> new RuntimeException("Le panier " + command.cartId.value() + " n'existe pas."));
        cart.setPromoCodeId(null);
        return new Result(cartProvider.write(cart));
    }

    public record Result(
            Cart cart
    ) {
    }

    public record Command(
            CartId cartId
    ) {
    }

}
