package me.noynto.eosa.application;

import me.noynto.eosa.cart.Cart;
import me.noynto.eosa.cart.CartItem;
import me.noynto.eosa.cart.CartProvider;
import me.noynto.eosa.cart.CartShippingRule;
import me.noynto.eosa.cart.CartShippingRuleProvider;
import me.noynto.eosa.shared.CartId;
import me.noynto.eosa.shared.CartItemId;
import me.noynto.eosa.shared.ImageId;
import me.noynto.eosa.shared.JewelId;
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
class RemoveJewelFromCartTest {

    @Mock CartProvider cartProvider;
    @Mock CartShippingRuleProvider shippingRuleProvider;

    @BeforeEach
    void setUp() {
        lenient().when(shippingRuleProvider.get()).thenReturn(defaultRule());
    }

    @Test
    void handle_removesItemFromCart() {
        var cartId = new CartId("cart1");
        var itemId = new CartItemId("item1");
        var cart = cartWith(cartId);
        cart.setItems(List.of(new CartItem(
                itemId, new JewelId("prod1"), "Lune", new BigDecimal("29.90"), new ImageId("img1"), 1,
                null, null, null, List.of()
        )));
        when(cartProvider.read(cartId)).thenReturn(Optional.of(cart));
        when(cartProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        new RemoveJewelFromCart(cartProvider, shippingRuleProvider).handle(
                new RemoveJewelFromCart.Command(cartId, itemId)
        );

        verify(cartProvider).write(argThat(c -> c.getItems().isEmpty()));
    }

    @Test
    void handle_keepsOtherItemsIntact() {
        var cartId = new CartId("cart1");
        var itemId = new CartItemId("item1");
        var otherItemId = new CartItemId("item2");
        var cart = cartWith(cartId);
        cart.setItems(List.of(
                new CartItem(itemId, new JewelId("prod1"), "Lune", new BigDecimal("29.90"), new ImageId("img1"), 1, null, null, null, List.of()),
                new CartItem(otherItemId, new JewelId("prod2"), "Soleil", new BigDecimal("39.90"), new ImageId("img2"), 1, null, null, null, List.of())
        ));
        when(cartProvider.read(cartId)).thenReturn(Optional.of(cart));
        when(cartProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        new RemoveJewelFromCart(cartProvider, shippingRuleProvider).handle(
                new RemoveJewelFromCart.Command(cartId, itemId)
        );

        verify(cartProvider).write(argThat(c ->
                c.getItems().size() == 1 &&
                c.getItems().getFirst().id().equals(otherItemId)
        ));
    }

    @Test
    void handle_throwsWhenCartIdIsNull() {
        assertThrows(RuntimeException.class, () ->
                new RemoveJewelFromCart(cartProvider, shippingRuleProvider).handle(
                        new RemoveJewelFromCart.Command(null, new CartItemId("item1"))
                )
        );
    }

    @Test
    void handle_throwsWhenItemIdIsNull() {
        assertThrows(RuntimeException.class, () ->
                new RemoveJewelFromCart(cartProvider, shippingRuleProvider).handle(
                        new RemoveJewelFromCart.Command(new CartId("cart1"), null)
                )
        );
    }

    @Test
    void handle_throwsWhenCartNotFound() {
        var cartId = new CartId("unknown");
        when(cartProvider.read(cartId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                new RemoveJewelFromCart(cartProvider, shippingRuleProvider).handle(
                        new RemoveJewelFromCart.Command(cartId, new CartItemId("item1"))
                )
        );
    }

    private Cart cartWith(CartId id) {
        var cart = new Cart();
        cart.setId(id);
        return cart;
    }

    private CartShippingRule defaultRule() {
        var rule = new CartShippingRule();

        rule.setFreeThreshold(new BigDecimal("60"));
        rule.setAmount(new BigDecimal("5"));
        return rule;
    }

}
