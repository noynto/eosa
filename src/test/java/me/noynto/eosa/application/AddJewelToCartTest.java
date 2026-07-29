package me.noynto.eosa.application;

import me.noynto.eosa.cart.Cart;
import me.noynto.eosa.cart.CartItem;
import me.noynto.eosa.cart.CartProvider;
import me.noynto.eosa.cart.CartShippingRule;
import me.noynto.eosa.cart.CartShippingRuleProvider;
import me.noynto.eosa.jewel.Jewel;
import me.noynto.eosa.jewel.JewelProvider;
import me.noynto.eosa.shared.CartId;
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
class AddJewelToCartTest {

    @Mock CartProvider cartProvider;
    @Mock JewelProvider jewelProvider;
    @Mock CartShippingRuleProvider shippingRuleProvider;

    @BeforeEach
    void setUp() {
        lenient().when(shippingRuleProvider.get()).thenReturn(defaultRule());
    }

    @Test
    void handle_addsNewItemWithSnapshot() {
        var cartId = new CartId("cart1");
        var jewelId = new JewelId("prod1");
        var cart = cartWith(cartId);
        var jewel = jewelWith(jewelId, "Lune", new BigDecimal("29.90"), "img1");
        when(cartProvider.read(cartId)).thenReturn(Optional.of(cart));
        when(jewelProvider.read(jewelId)).thenReturn(Optional.of(jewel));
        when(cartProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        new AddJewelToCart(cartProvider, jewelProvider, shippingRuleProvider).handle(
                new AddJewelToCart.Command(cartId, jewelId)
        );

        verify(cartProvider).write(argThat(c ->
                c.getItems().size() == 1 &&
                c.getItems().getFirst().jewelId().equals(jewelId) &&
                "Lune".equals(c.getItems().getFirst().name()) &&
                new BigDecimal("29.90").equals(c.getItems().getFirst().price()) &&
                c.getItems().getFirst().quantity() == 1
        ));
    }

    @Test
    void handle_incrementsQuantityWhenJewelAlreadyInCart() {
        var cartId = new CartId("cart1");
        var jewelId = new JewelId("prod1");
        var cart = cartWith(cartId);
        cart.setItems(List.of(new CartItem(jewelId, "Lune", new BigDecimal("29.90"), new ImageId("img1"), 2)));
        var jewel = jewelWith(jewelId, "Lune", new BigDecimal("29.90"), "img1");
        when(cartProvider.read(cartId)).thenReturn(Optional.of(cart));
        when(jewelProvider.read(jewelId)).thenReturn(Optional.of(jewel));
        when(cartProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        new AddJewelToCart(cartProvider, jewelProvider, shippingRuleProvider).handle(
                new AddJewelToCart.Command(cartId, jewelId)
        );

        verify(cartProvider).write(argThat(c ->
                c.getItems().size() == 1 &&
                c.getItems().getFirst().quantity() == 3
        ));
    }

    @Test
    void handle_throwsWhenCartIdIsNull() {
        assertThrows(RuntimeException.class, () ->
                new AddJewelToCart(cartProvider, jewelProvider, shippingRuleProvider).handle(
                        new AddJewelToCart.Command(null, new JewelId("prod1"))
                )
        );
    }

    @Test
    void handle_throwsWhenJewelIdIsNull() {
        assertThrows(RuntimeException.class, () ->
                new AddJewelToCart(cartProvider, jewelProvider, shippingRuleProvider).handle(
                        new AddJewelToCart.Command(new CartId("cart1"), null)
                )
        );
    }

    @Test
    void handle_throwsWhenCartNotFound() {
        var cartId = new CartId("unknown");
        when(cartProvider.read(cartId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                new AddJewelToCart(cartProvider, jewelProvider, shippingRuleProvider).handle(
                        new AddJewelToCart.Command(cartId, new JewelId("prod1"))
                )
        );
    }

    @Test
    void handle_throwsWhenJewelNotFound() {
        var cartId = new CartId("cart1");
        var jewelId = new JewelId("unknown");
        when(cartProvider.read(cartId)).thenReturn(Optional.of(cartWith(cartId)));
        when(jewelProvider.read(jewelId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                new AddJewelToCart(cartProvider, jewelProvider, shippingRuleProvider).handle(
                        new AddJewelToCart.Command(cartId, jewelId)
                )
        );
    }

    private Cart cartWith(CartId id) {
        var cart = new Cart();
        cart.setId(id);
        return cart;
    }

    private Jewel jewelWith(JewelId id, String name, BigDecimal price, String imageId) {
        var jewel = new Jewel();
        jewel.setId(id);
        jewel.setName(name);
        jewel.setPrice(price);
        jewel.setImageIds(List.of(new ImageId(imageId)));
        return jewel;
    }

    private CartShippingRule defaultRule() {
        var rule = new CartShippingRule();
        
        rule.setFreeThreshold(new BigDecimal("60"));
        rule.setAmount(new BigDecimal("5"));
        return rule;
    }

}