package me.noynto.eosa.application;

import me.noynto.eosa.cart.Cart;
import me.noynto.eosa.cart.CartProvider;
import me.noynto.eosa.cart.CartShippingRule;
import me.noynto.eosa.cart.CartShippingRuleProvider;
import me.noynto.eosa.shared.CartId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadCartTest {

    @Mock CartProvider cartProvider;
    @Mock CartShippingRuleProvider shippingRuleProvider;

    @Test
    void handle_returnsTheExistingCartWithItsShipping() {
        var cartId = new CartId("cart");
        var cart = new Cart();
        when(cartProvider.read(cartId)).thenReturn(Optional.of(cart));
        var rule = new CartShippingRule();
        rule.setFreeThreshold(new BigDecimal("60"));
        rule.setAmount(new BigDecimal("5"));
        when(shippingRuleProvider.get()).thenReturn(rule);

        var result = new ReadCart(cartProvider, shippingRuleProvider).handle(new ReadCart.Command(cartId));

        assertSame(cart, result.cart());
        assertNotNull(cart.getShipping());
    }

    @Test
    void handle_neverCreatesACart() {
        var cartId = new CartId("unknown");
        when(cartProvider.read(cartId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                new ReadCart(cartProvider, shippingRuleProvider).handle(new ReadCart.Command(cartId)));
        verify(cartProvider, never()).write(any());
    }

    @Test
    void handle_requiresACartId() {
        assertThrows(RuntimeException.class, () ->
                new ReadCart(cartProvider, shippingRuleProvider).handle(new ReadCart.Command(null)));
    }

}
