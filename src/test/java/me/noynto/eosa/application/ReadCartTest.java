package me.noynto.eosa.application;

import me.noynto.eosa.cart.Cart;
import me.noynto.eosa.cart.CartItem;
import me.noynto.eosa.cart.CartProvider;
import me.noynto.eosa.cart.CartShippingRule;
import me.noynto.eosa.cart.CartShippingRuleProvider;
import me.noynto.eosa.promo.PromoCode;
import me.noynto.eosa.giftcard.GiftCardProvider;
import me.noynto.eosa.promo.PromoCodeProvider;
import me.noynto.eosa.promo.PromoCodeType;
import me.noynto.eosa.shared.CartId;
import me.noynto.eosa.shared.CartItemId;
import me.noynto.eosa.shared.JewelId;
import me.noynto.eosa.shared.PromoCodeId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadCartTest {

    @Mock CartProvider cartProvider;
    @Mock CartShippingRuleProvider shippingRuleProvider;
    @Mock PromoCodeProvider promoCodeProvider;
    @Mock GiftCardProvider giftCardProvider;

    private final Instant now = Instant.parse("2026-11-14T10:00:00Z");

    private ReadCart readCart() {
        return new ReadCart(cartProvider, shippingRuleProvider, promoCodeProvider, new InMemoryOrders(),
                giftCardProvider, Clock.fixed(now, ZoneOffset.UTC));
    }

    private Cart cartOf(String price, PromoCodeId promoCodeId) {
        var cart = new Cart();
        cart.setItems(List.of(new CartItem(new CartItemId("i"), new JewelId("j"), "Collier Lune", new BigDecimal(price),
                null, 1, null, null, null, List.of())));
        cart.setPromoCodeId(promoCodeId);
        return cart;
    }

    private void shippingRule() {
        var rule = new CartShippingRule();
        rule.setFreeThreshold(new BigDecimal("60"));
        rule.setAmount(new BigDecimal("5"));
        when(shippingRuleProvider.get()).thenReturn(rule);
    }

    private PromoCode code(String type, String value, String minimum) {
        var promoCode = new PromoCode();
        promoCode.setId(new PromoCodeId("p-1"));
        promoCode.setCode("BIENVENUE10");
        promoCode.setType(PromoCodeType.valueOf(type));
        promoCode.setValue(new BigDecimal(value));
        promoCode.setMinimumAmount(minimum == null ? null : new BigDecimal(minimum));
        return promoCode;
    }

    @Test
    void handle_discountsTheCartWithItsPromoCode() {
        var cartId = new CartId("cart");
        when(cartProvider.read(cartId)).thenReturn(Optional.of(cartOf("40", new PromoCodeId("p-1"))));
        shippingRule();
        when(promoCodeProvider.read(new PromoCodeId("p-1"))).thenReturn(Optional.of(code("PERCENT", "10", null)));

        var result = readCart().handle(new ReadCart.Command(cartId));

        assertEquals(new BigDecimal("4.00"), result.discount());
        assertNull(result.refusal());
        assertEquals("BIENVENUE10", result.promoCode().getCode());
        // La livraison reste calculée sur les bijoux avant remise.
        assertEquals(new BigDecimal("5"), result.cart().getShipping().getAmount());
    }

    @Test
    void handle_keepsTheCodeButDiscountsNothingWhenItNoLongerApplies() {
        var cartId = new CartId("cart");
        when(cartProvider.read(cartId)).thenReturn(Optional.of(cartOf("40", new PromoCodeId("p-1"))));
        shippingRule();
        when(promoCodeProvider.read(new PromoCodeId("p-1"))).thenReturn(Optional.of(code("PERCENT", "10", "60")));

        var result = readCart().handle(new ReadCart.Command(cartId));

        assertEquals(0, result.discount().signum());
        assertTrue(result.refusal().contains("60,00"), result.refusal());
        assertEquals("BIENVENUE10", result.promoCode().getCode());
        verify(cartProvider, never()).write(any());
    }

    @Test
    void handle_hasNoDiscountWithoutACode() {
        var cartId = new CartId("cart");
        when(cartProvider.read(cartId)).thenReturn(Optional.of(cartOf("40", null)));
        shippingRule();

        var result = readCart().handle(new ReadCart.Command(cartId));

        assertEquals(0, result.discount().signum());
        assertNull(result.promoCode());
        assertNull(result.refusal());
    }

    @Test
    void handle_returnsTheExistingCartWithItsShipping() {
        var cartId = new CartId("cart");
        var cart = new Cart();
        when(cartProvider.read(cartId)).thenReturn(Optional.of(cart));
        var rule = new CartShippingRule();
        rule.setFreeThreshold(new BigDecimal("60"));
        rule.setAmount(new BigDecimal("5"));
        when(shippingRuleProvider.get()).thenReturn(rule);

        var result = readCart().handle(new ReadCart.Command(cartId));

        assertSame(cart, result.cart());
        assertNotNull(cart.getShipping());
    }

    @Test
    void handle_neverCreatesACart() {
        var cartId = new CartId("unknown");
        when(cartProvider.read(cartId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                readCart().handle(new ReadCart.Command(cartId)));
        verify(cartProvider, never()).write(any());
    }

    @Test
    void handle_requiresACartId() {
        assertThrows(RuntimeException.class, () ->
                readCart().handle(new ReadCart.Command(null)));
    }

}
