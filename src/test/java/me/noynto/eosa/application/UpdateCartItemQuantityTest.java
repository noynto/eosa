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
class UpdateCartItemQuantityTest {

    @Mock CartProvider cartProvider;
    @Mock CartShippingRuleProvider shippingRuleProvider;

    @BeforeEach
    void setUp() {
        lenient().when(shippingRuleProvider.get()).thenReturn(defaultRule());
    }

    @Test
    void handle_updatesQuantity() {
        var cartId = new CartId("cart1");
        var itemId = new CartItemId("item1");
        var cart = cartWithItem(cartId, itemId, 1);
        when(cartProvider.read(cartId)).thenReturn(Optional.of(cart));
        when(cartProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        new UpdateCartItemQuantity(cartProvider, shippingRuleProvider).handle(
                new UpdateCartItemQuantity.Command(cartId, itemId, 4)
        );

        verify(cartProvider).write(argThat(c ->
                c.getItems().size() == 1 &&
                c.getItems().getFirst().quantity() == 4
        ));
    }

    @Test
    void handle_removesItemWhenQuantityIsZero() {
        var cartId = new CartId("cart1");
        var itemId = new CartItemId("item1");
        var cart = cartWithItem(cartId, itemId, 2);
        when(cartProvider.read(cartId)).thenReturn(Optional.of(cart));
        when(cartProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        new UpdateCartItemQuantity(cartProvider, shippingRuleProvider).handle(
                new UpdateCartItemQuantity.Command(cartId, itemId, 0)
        );

        verify(cartProvider).write(argThat(c -> c.getItems().isEmpty()));
    }

    @Test
    void handle_preservesSnapshotData() {
        var cartId = new CartId("cart1");
        var itemId = new CartItemId("item1");
        var cart = cartWithItem(cartId, itemId, 1);
        when(cartProvider.read(cartId)).thenReturn(Optional.of(cart));
        when(cartProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        new UpdateCartItemQuantity(cartProvider, shippingRuleProvider).handle(
                new UpdateCartItemQuantity.Command(cartId, itemId, 3)
        );

        verify(cartProvider).write(argThat(c ->
                "Lune".equals(c.getItems().getFirst().name()) &&
                new BigDecimal("29.90").equals(c.getItems().getFirst().price())
        ));
    }

    @Test
    void handle_throwsWhenQuantityIsNegative() {
        assertThrows(RuntimeException.class, () ->
                new UpdateCartItemQuantity(cartProvider, shippingRuleProvider).handle(
                        new UpdateCartItemQuantity.Command(new CartId("cart1"), new CartItemId("item1"), -1)
                )
        );
    }

    @Test
    void handle_throwsWhenCartIdIsNull() {
        assertThrows(RuntimeException.class, () ->
                new UpdateCartItemQuantity(cartProvider, shippingRuleProvider).handle(
                        new UpdateCartItemQuantity.Command(null, new CartItemId("item1"), 2)
                )
        );
    }

    @Test
    void handle_throwsWhenItemIdIsNull() {
        assertThrows(RuntimeException.class, () ->
                new UpdateCartItemQuantity(cartProvider, shippingRuleProvider).handle(
                        new UpdateCartItemQuantity.Command(new CartId("cart1"), null, 2)
                )
        );
    }

    @Test
    void handle_throwsWhenCartNotFound() {
        var cartId = new CartId("unknown");
        when(cartProvider.read(cartId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                new UpdateCartItemQuantity(cartProvider, shippingRuleProvider).handle(
                        new UpdateCartItemQuantity.Command(cartId, new CartItemId("item1"), 2)
                )
        );
    }

    @Test
    void handle_throwsWhenItemNotInCart() {
        var cartId = new CartId("cart1");
        var cart = new Cart();
        cart.setId(cartId);
        when(cartProvider.read(cartId)).thenReturn(Optional.of(cart));

        assertThrows(RuntimeException.class, () ->
                new UpdateCartItemQuantity(cartProvider, shippingRuleProvider).handle(
                        new UpdateCartItemQuantity.Command(cartId, new CartItemId("unknown"), 2)
                )
        );
    }

    private Cart cartWithItem(CartId cartId, CartItemId itemId, int quantity) {
        var cart = new Cart();
        cart.setId(cartId);
        cart.setItems(List.of(new CartItem(
                itemId, new JewelId("prod1"), "Lune", new BigDecimal("29.90"), new ImageId("img1"), quantity,
                null, null, null, List.of()
        )));
        return cart;
    }

    private CartShippingRule defaultRule() {
        var rule = new CartShippingRule();

        rule.setFreeThreshold(new BigDecimal("60"));
        rule.setAmount(new BigDecimal("5"));
        return rule;
    }

}
