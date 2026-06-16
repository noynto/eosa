package me.noynto.eosa.application;

import me.noynto.eosa.cart.Cart;
import me.noynto.eosa.cart.CartItem;
import me.noynto.eosa.cart.CartProvider;
import me.noynto.eosa.cart.CartShippingRule;
import me.noynto.eosa.cart.CartShippingRuleProvider;
import me.noynto.eosa.product.Product;
import me.noynto.eosa.product.ProductProvider;
import me.noynto.eosa.shared.CartId;
import me.noynto.eosa.shared.ImageId;
import me.noynto.eosa.shared.ProductId;
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
class AddProductToCartTest {

    @Mock CartProvider cartProvider;
    @Mock ProductProvider productProvider;
    @Mock CartShippingRuleProvider shippingRuleProvider;

    @BeforeEach
    void setUp() {
        lenient().when(shippingRuleProvider.get()).thenReturn(defaultRule());
    }

    @Test
    void handle_addsNewItemWithSnapshot() {
        var cartId = new CartId("cart1");
        var productId = new ProductId("prod1");
        var cart = cartWith(cartId);
        var product = productWith(productId, "Lune", new BigDecimal("29.90"), "img1");
        when(cartProvider.read(cartId)).thenReturn(Optional.of(cart));
        when(productProvider.read(productId)).thenReturn(Optional.of(product));
        when(cartProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        new AddProductToCart(cartProvider, productProvider, shippingRuleProvider).handle(
                new AddProductToCart.Command(cartId, productId)
        );

        verify(cartProvider).write(argThat(c ->
                c.getItems().size() == 1 &&
                c.getItems().getFirst().productId().equals(productId) &&
                "Lune".equals(c.getItems().getFirst().name()) &&
                new BigDecimal("29.90").equals(c.getItems().getFirst().price()) &&
                c.getItems().getFirst().quantity() == 1
        ));
    }

    @Test
    void handle_incrementsQuantityWhenProductAlreadyInCart() {
        var cartId = new CartId("cart1");
        var productId = new ProductId("prod1");
        var cart = cartWith(cartId);
        cart.setItems(List.of(new CartItem(productId, "Lune", new BigDecimal("29.90"), new ImageId("img1"), 2)));
        var product = productWith(productId, "Lune", new BigDecimal("29.90"), "img1");
        when(cartProvider.read(cartId)).thenReturn(Optional.of(cart));
        when(productProvider.read(productId)).thenReturn(Optional.of(product));
        when(cartProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        new AddProductToCart(cartProvider, productProvider, shippingRuleProvider).handle(
                new AddProductToCart.Command(cartId, productId)
        );

        verify(cartProvider).write(argThat(c ->
                c.getItems().size() == 1 &&
                c.getItems().getFirst().quantity() == 3
        ));
    }

    @Test
    void handle_throwsWhenCartIdIsNull() {
        assertThrows(RuntimeException.class, () ->
                new AddProductToCart(cartProvider, productProvider, shippingRuleProvider).handle(
                        new AddProductToCart.Command(null, new ProductId("prod1"))
                )
        );
    }

    @Test
    void handle_throwsWhenProductIdIsNull() {
        assertThrows(RuntimeException.class, () ->
                new AddProductToCart(cartProvider, productProvider, shippingRuleProvider).handle(
                        new AddProductToCart.Command(new CartId("cart1"), null)
                )
        );
    }

    @Test
    void handle_throwsWhenCartNotFound() {
        var cartId = new CartId("unknown");
        when(cartProvider.read(cartId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                new AddProductToCart(cartProvider, productProvider, shippingRuleProvider).handle(
                        new AddProductToCart.Command(cartId, new ProductId("prod1"))
                )
        );
    }

    @Test
    void handle_throwsWhenProductNotFound() {
        var cartId = new CartId("cart1");
        var productId = new ProductId("unknown");
        when(cartProvider.read(cartId)).thenReturn(Optional.of(cartWith(cartId)));
        when(productProvider.read(productId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                new AddProductToCart(cartProvider, productProvider, shippingRuleProvider).handle(
                        new AddProductToCart.Command(cartId, productId)
                )
        );
    }

    private Cart cartWith(CartId id) {
        var cart = new Cart();
        cart.setId(id);
        return cart;
    }

    private Product productWith(ProductId id, String name, BigDecimal price, String imageId) {
        var product = new Product();
        product.setId(id);
        product.setName(name);
        product.setPrice(price);
        product.setImageIds(List.of(new ImageId(imageId)));
        return product;
    }

    private CartShippingRule defaultRule() {
        var rule = new CartShippingRule();
        
        rule.setFreeThreshold(new BigDecimal("60"));
        rule.setAmount(new BigDecimal("5"));
        return rule;
    }

}