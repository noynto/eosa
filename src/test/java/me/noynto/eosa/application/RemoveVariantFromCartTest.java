package me.noynto.eosa.application;

import me.noynto.eosa.cart.Cart;
import me.noynto.eosa.cart.CartItem;
import me.noynto.eosa.cart.CartProvider;
import me.noynto.eosa.cart.CartShippingRule;
import me.noynto.eosa.cart.CartShippingRuleProvider;
import me.noynto.eosa.shared.CartId;
import me.noynto.eosa.shared.ImageId;
import me.noynto.eosa.shared.VariantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemoveVariantFromCartTest {

    @Mock CartProvider cartProvider;
    @Mock CartShippingRuleProvider shippingRuleProvider;

    @BeforeEach
    void setUp() {
        lenient().when(shippingRuleProvider.get()).thenReturn(defaultRule());
    }

    @Test
    void handle_removesMatchingItem() {
        var cartId = new CartId("cart1");
        var variantId = new VariantId("var1");
        var cart = new Cart();
        cart.setId(cartId);
        cart.setItems(List.of(new CartItem(variantId, null, "Lune", new BigDecimal("29.90"), null, new ImageId("img1"), 2)));
        when(cartProvider.read(cartId)).thenReturn(Optional.of(cart));
        when(cartProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        new RemoveVariantFromCart(cartProvider, shippingRuleProvider).handle(
                new RemoveVariantFromCart.Command(cartId, variantId)
        );

        verify(cartProvider).write(argThat(c -> c.getItems().isEmpty()));
    }

    @Test
    void handle_throwsWhenCartIdIsNull() {
        assertThrows(RuntimeException.class, () ->
                new RemoveVariantFromCart(cartProvider, shippingRuleProvider).handle(
                        new RemoveVariantFromCart.Command(null, new VariantId("var1"))
                )
        );
    }

    @Test
    void handle_throwsWhenVariantIdIsNull() {
        assertThrows(RuntimeException.class, () ->
                new RemoveVariantFromCart(cartProvider, shippingRuleProvider).handle(
                        new RemoveVariantFromCart.Command(new CartId("cart1"), null)
                )
        );
    }

    @Test
    void handle_throwsWhenCartNotFound() {
        var cartId = new CartId("unknown");
        when(cartProvider.read(cartId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                new RemoveVariantFromCart(cartProvider, shippingRuleProvider).handle(
                        new RemoveVariantFromCart.Command(cartId, new VariantId("var1"))
                )
        );
    }

    private CartShippingRule defaultRule() {
        var rule = new CartShippingRule();
        rule.setFreeThreshold(new BigDecimal("60"));
        rule.setAmount(new BigDecimal("5"));
        return rule;
    }

}
