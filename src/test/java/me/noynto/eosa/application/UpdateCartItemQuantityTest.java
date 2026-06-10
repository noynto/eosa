package me.noynto.eosa.application;

import me.noynto.eosa.cart.Cart;
import me.noynto.eosa.cart.CartItem;
import me.noynto.eosa.cart.CartProvider;
import me.noynto.eosa.shared.CartId;
import me.noynto.eosa.shared.ImageId;
import me.noynto.eosa.shared.ProductId;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateCartItemQuantityTest {

    @Mock CartProvider cartProvider;

    @Test
    void handle_updatesQuantity() {
        var cartId = new CartId("cart1");
        var productId = new ProductId("prod1");
        var cart = cartWithItem(cartId, productId, 1);
        when(cartProvider.read(cartId)).thenReturn(Optional.of(cart));
        when(cartProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        new UpdateCartItemQuantity(cartProvider).handle(
                new UpdateCartItemQuantity.Command(cartId, productId, 4)
        );

        verify(cartProvider).write(argThat(c ->
                c.getItems().size() == 1 &&
                c.getItems().getFirst().quantity() == 4
        ));
    }

    @Test
    void handle_removesItemWhenQuantityIsZero() {
        var cartId = new CartId("cart1");
        var productId = new ProductId("prod1");
        var cart = cartWithItem(cartId, productId, 2);
        when(cartProvider.read(cartId)).thenReturn(Optional.of(cart));
        when(cartProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        new UpdateCartItemQuantity(cartProvider).handle(
                new UpdateCartItemQuantity.Command(cartId, productId, 0)
        );

        verify(cartProvider).write(argThat(c -> c.getItems().isEmpty()));
    }

    @Test
    void handle_preservesSnapshotData() {
        var cartId = new CartId("cart1");
        var productId = new ProductId("prod1");
        var cart = cartWithItem(cartId, productId, 1);
        when(cartProvider.read(cartId)).thenReturn(Optional.of(cart));
        when(cartProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        new UpdateCartItemQuantity(cartProvider).handle(
                new UpdateCartItemQuantity.Command(cartId, productId, 3)
        );

        verify(cartProvider).write(argThat(c ->
                "Lune".equals(c.getItems().getFirst().name()) &&
                new BigDecimal("29.90").equals(c.getItems().getFirst().price())
        ));
    }

    @Test
    void handle_throwsWhenQuantityIsNegative() {
        assertThrows(RuntimeException.class, () ->
                new UpdateCartItemQuantity(cartProvider).handle(
                        new UpdateCartItemQuantity.Command(new CartId("cart1"), new ProductId("prod1"), -1)
                )
        );
    }

    @Test
    void handle_throwsWhenCartIdIsNull() {
        assertThrows(RuntimeException.class, () ->
                new UpdateCartItemQuantity(cartProvider).handle(
                        new UpdateCartItemQuantity.Command(null, new ProductId("prod1"), 2)
                )
        );
    }

    @Test
    void handle_throwsWhenProductIdIsNull() {
        assertThrows(RuntimeException.class, () ->
                new UpdateCartItemQuantity(cartProvider).handle(
                        new UpdateCartItemQuantity.Command(new CartId("cart1"), null, 2)
                )
        );
    }

    @Test
    void handle_throwsWhenCartNotFound() {
        var cartId = new CartId("unknown");
        when(cartProvider.read(cartId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                new UpdateCartItemQuantity(cartProvider).handle(
                        new UpdateCartItemQuantity.Command(cartId, new ProductId("prod1"), 2)
                )
        );
    }

    @Test
    void handle_throwsWhenProductNotInCart() {
        var cartId = new CartId("cart1");
        var cart = new Cart();
        cart.setId(cartId);
        when(cartProvider.read(cartId)).thenReturn(Optional.of(cart));

        assertThrows(RuntimeException.class, () ->
                new UpdateCartItemQuantity(cartProvider).handle(
                        new UpdateCartItemQuantity.Command(cartId, new ProductId("unknown"), 2)
                )
        );
    }

    private Cart cartWithItem(CartId cartId, ProductId productId, int quantity) {
        var cart = new Cart();
        cart.setId(cartId);
        cart.setItems(List.of(new CartItem(productId, "Lune", new BigDecimal("29.90"), new ImageId("img1"), quantity)));
        return cart;
    }

}