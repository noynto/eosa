package me.noynto.eosa.application;

import me.noynto.eosa.cart.Cart;
import me.noynto.eosa.cart.CartItem;
import me.noynto.eosa.cart.CartProvider;
import me.noynto.eosa.cart.CartShippingRule;
import me.noynto.eosa.cart.CartShippingRuleProvider;
import me.noynto.eosa.checkout.Checkout;
import me.noynto.eosa.checkout.CheckoutProvider;
import me.noynto.eosa.promo.PromoCode;
import me.noynto.eosa.promo.PromoCodeProvider;
import me.noynto.eosa.promo.PromoCodeType;
import me.noynto.eosa.shared.CartId;
import me.noynto.eosa.shared.CartItemId;
import me.noynto.eosa.shared.JewelId;
import me.noynto.eosa.shared.PromoCodeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InitiateCheckoutTest {

    @Mock CartProvider cartProvider;
    @Mock CheckoutProvider checkoutProvider;
    @Mock CartShippingRuleProvider shippingRuleProvider;
    @Mock PromoCodeProvider promoCodeProvider;

    private final Instant now = Instant.parse("2026-11-14T10:00:00Z");
    private final CartId cartId = new CartId("cart");
    private final PromoCodeId promoCodeId = new PromoCodeId("p-1");
    private Cart cart;

    @BeforeEach
    void setUp() {
        cart = new Cart();
        cart.setId(cartId);
        cart.setItems(List.of(new CartItem(new CartItemId("i"), new JewelId("j"), "Collier Lune", new BigDecimal("40"),
                null, 1, null, null, null, List.of())));
        when(cartProvider.read(cartId)).thenReturn(Optional.of(cart));
        var rule = new CartShippingRule();
        rule.setFreeThreshold(new BigDecimal("60"));
        rule.setAmount(new BigDecimal("5"));
        when(shippingRuleProvider.get()).thenReturn(rule);
        when(checkoutProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Checkout initiate() throws Exception {
        var useCase = new InitiateCheckout(cartProvider, checkoutProvider, shippingRuleProvider, promoCodeProvider,
                new InMemoryOrders(), Clock.fixed(now, ZoneOffset.UTC));
        useCase.handle(new InitiateCheckout.Command(cartId));
        var captor = ArgumentCaptor.forClass(Checkout.class);
        verify(checkoutProvider).write(captor.capture());
        return captor.getValue();
    }

    private PromoCode promoCode(String minimum) {
        var promoCode = new PromoCode();
        promoCode.setId(promoCodeId);
        promoCode.setCode("BIENVENUE10");
        promoCode.setType(PromoCodeType.PERCENT);
        promoCode.setValue(new BigDecimal("10"));
        promoCode.setMinimumAmount(minimum == null ? null : new BigDecimal(minimum));
        when(promoCodeProvider.read(promoCodeId)).thenReturn(Optional.of(promoCode));
        return promoCode;
    }

    @Test
    void handle_sendsTheDiscountAndTheCodeToTheCheckout() throws Exception {
        cart.setPromoCodeId(promoCodeId);
        promoCode(null);

        var checkout = initiate();

        assertEquals(new BigDecimal("4.00"), checkout.getDiscountAmount());
        assertEquals(promoCodeId, checkout.getPromoCodeId());
        assertEquals("BIENVENUE10", checkout.getPromoCode());
        // Les lignes gardent leur prix catalogue : la remise est portée à part.
        assertEquals(new BigDecimal("40"), checkout.getItems().getFirst().getUnitPrice());
        assertEquals(new BigDecimal("5"), checkout.getShippingAmount());
    }

    @Test
    void handle_sendsNoDiscountWhenTheCodeNoLongerApplies() throws Exception {
        cart.setPromoCodeId(promoCodeId);
        promoCode("60");

        var checkout = initiate();

        assertNull(checkout.getDiscountAmount());
        assertNull(checkout.getPromoCodeId());
        assertNull(checkout.getPromoCode());
    }

    @Test
    void handle_sendsNoDiscountWithoutACode() throws Exception {
        var checkout = initiate();

        assertNull(checkout.getDiscountAmount());
        assertNull(checkout.getPromoCodeId());
    }

}
