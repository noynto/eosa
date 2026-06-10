package me.noynto.eosa.cart;

import me.noynto.eosa.shared.CartId;

import java.util.Optional;

public interface CartProvider {

    Optional<Cart> read(CartId cartId);

    Cart write(Cart cart);

}