package me.noynto.eosa.application;

import me.noynto.eosa.cart.Cart;
import me.noynto.eosa.cart.CartProvider;
import me.noynto.eosa.shared.CartId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetOrCreateCartTest {

    @Mock CartProvider cartProvider;

    @Test
    void handle_returnsExistingCart() {
        var cartId = new CartId("abc");
        var existing = cartWith(cartId);
        when(cartProvider.read(cartId)).thenReturn(Optional.of(existing));

        var result = new GetOrCreateCart(cartProvider).handle(new GetOrCreateCart.Command(cartId));

        assertSame(existing, result);
    }

    @Test
    void handle_createsNewCartWhenCartIdIsNull() {
        var created = cartWith(new CartId("new"));
        when(cartProvider.write(any())).thenReturn(created);

        var result = new GetOrCreateCart(cartProvider).handle(new GetOrCreateCart.Command(null));

        assertNotNull(result);
        verify(cartProvider).write(any());
    }

    @Test
    void handle_createsNewCartWhenCartNotFound() {
        var cartId = new CartId("unknown");
        var created = cartWith(new CartId("new"));
        when(cartProvider.read(cartId)).thenReturn(Optional.empty());
        when(cartProvider.write(any())).thenReturn(created);

        var result = new GetOrCreateCart(cartProvider).handle(new GetOrCreateCart.Command(cartId));

        assertNotNull(result);
        verify(cartProvider).write(any());
    }

    private Cart cartWith(CartId id) {
        var cart = new Cart();
        cart.setId(id);
        return cart;
    }

}