package me.noynto.eosa.application;

import me.noynto.eosa.cart.Cart;
import me.noynto.eosa.cart.CartItem;
import me.noynto.eosa.cart.CartProvider;
import me.noynto.eosa.cart.CartShippingRule;
import me.noynto.eosa.cart.CartShippingRuleProvider;
import me.noynto.eosa.checkout.Checkout;
import me.noynto.eosa.checkout.CheckoutItem;
import me.noynto.eosa.checkout.CheckoutSession;
import me.noynto.eosa.promo.PromoCode;
import me.noynto.eosa.promo.PromoCodeProvider;
import me.noynto.eosa.promo.PromoCodeType;
import me.noynto.eosa.service.OrdersFromCheckouts;
import me.noynto.eosa.shared.CartId;
import me.noynto.eosa.shared.CartItemId;
import me.noynto.eosa.shared.CheckoutSessionId;
import me.noynto.eosa.shared.JewelId;
import me.noynto.eosa.shared.GiftCardId;
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
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplyPromoCodeTest {

    @Mock CartProvider cartProvider;
    @Mock PromoCodeProvider promoCodeProvider;
    @Mock CartShippingRuleProvider shippingRuleProvider;

    private final InMemoryOrders orders = new InMemoryOrders();
    private final Instant now = Instant.parse("2026-11-14T10:00:00Z");
    private final CartId cartId = new CartId("cart");
    private final PromoCodeId id = new PromoCodeId("p-1");

    private ApplyPromoCode useCase() {
        return new ApplyPromoCode(cartProvider, promoCodeProvider, orders, shippingRuleProvider, Clock.fixed(now, ZoneOffset.UTC));
    }

    private void cartOf(String price) {
        var cart = new Cart();
        cart.setId(cartId);
        cart.setItems(List.of(new CartItem(new CartItemId("i"), new JewelId("j"), "Collier Lune", new BigDecimal(price),
                null, 1, null, null, null, List.of())));
        when(cartProvider.read(cartId)).thenReturn(Optional.of(cart));
        var rule = new CartShippingRule();
        rule.setFreeThreshold(new BigDecimal("60"));
        rule.setAmount(new BigDecimal("5"));
        when(shippingRuleProvider.get()).thenReturn(rule);
        // Les cas de refus n'écrivent rien.
        lenient().when(cartProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private PromoCode promoCode(Integer maxUses) {
        var promoCode = new PromoCode();
        promoCode.setId(id);
        promoCode.setCode("BIENVENUE10");
        promoCode.setType(PromoCodeType.PERCENT);
        promoCode.setValue(new BigDecimal("10"));
        promoCode.setMaxUses(maxUses);
        when(promoCodeProvider.readIds("BIENVENUE10")).thenReturn(Stream.of(id));
        when(promoCodeProvider.read(id)).thenReturn(Optional.of(promoCode));
        return promoCode;
    }

    private void orderWithTheCode() {
        var session = new CheckoutSession();
        var sessionId = new CheckoutSessionId();
        sessionId.setStripe("cs_1");
        session.setId(sessionId);
        var item = new CheckoutItem();
        item.setName("Collier Lune");
        item.setUnitPrice(new BigDecimal("33.00"));
        item.setQuantity(1);
        var checkout = new Checkout();
        checkout.setSession(session);
        checkout.setItems(List.of(item));
        checkout.setDiscountAmount(new BigDecimal("3.30"));
        checkout.setPromoCodeId(id);
        checkout.setPromoCode("BIENVENUE10");
        orders.write(OrdersFromCheckouts.fromPaidCheckout(checkout, now));
    }

    @Test
    void handle_attachesTheCodeToTheCartAndReturnsTheDiscount() {
        cartOf("40");
        promoCode(null);

        var result = useCase().handle(new ApplyPromoCode.Command(cartId, "  bienvenue10 "));

        assertEquals(id, result.cart().getPromoCodeId());
        assertEquals(new BigDecimal("4.00"), result.discount());
        verify(cartProvider).write(any());
    }

    @Test
    void handle_replacesThePreviousCode() {
        cartOf("40");
        promoCode(null);
        // Le panier avait un autre code : le nouveau le remplace.
        var cart = cartProviderCart();
        cart.setPromoCodeId(new PromoCodeId("old"));

        useCase().handle(new ApplyPromoCode.Command(cartId, "BIENVENUE10"));

        assertEquals(id, cart.getPromoCodeId());
    }

    private Cart cartProviderCart() {
        return cartProvider.read(cartId).orElseThrow();
    }

    @Test
    void handle_refusesAnUnknownCodeWithoutTouchingTheCart() {
        cartOf("40");
        when(promoCodeProvider.readIds("INCONNU")).thenReturn(Stream.empty());

        var error = assertThrows(RuntimeException.class, () -> useCase().handle(new ApplyPromoCode.Command(cartId, "inconnu")));

        assertEquals("Ce code n'est pas valable.", error.getMessage());
        verify(cartProvider, never()).write(any());
    }

    @Test
    void handle_refusesACodeWhoseUsesAreExhausted() {
        cartOf("40");
        promoCode(1);
        orderWithTheCode();

        var error = assertThrows(RuntimeException.class, () -> useCase().handle(new ApplyPromoCode.Command(cartId, "BIENVENUE10")));

        assertEquals("Ce code n'est plus disponible.", error.getMessage());
        verify(cartProvider, never()).write(any());
    }

    @Test
    void handle_refusesACodeWhenAGiftCardIsAttached() {
        var cart = new Cart();
        cart.setId(cartId);
        cart.setItems(List.of(new CartItem(new CartItemId("i"), new JewelId("j"), "Collier Lune", new BigDecimal("40"),
                null, 1, null, null, null, List.of())));
        cart.setGiftCardId(new GiftCardId("card-1"));
        when(cartProvider.read(cartId)).thenReturn(Optional.of(cart));

        var error = assertThrows(RuntimeException.class, () -> useCase().handle(new ApplyPromoCode.Command(cartId, "BIENVENUE10")));

        assertEquals("Retirez d'abord la carte cadeau pour utiliser un code de promotion.", error.getMessage());
        verify(cartProvider, never()).write(any());
    }

    @Test
    void handle_refusesABlankCodeAnEmptyCartAndAMissingCart() {
        assertThrows(RuntimeException.class, () -> useCase().handle(new ApplyPromoCode.Command(cartId, "  ")));
        assertThrows(RuntimeException.class, () -> useCase().handle(new ApplyPromoCode.Command(null, "X")));
        when(cartProvider.read(cartId)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> useCase().handle(new ApplyPromoCode.Command(cartId, "BIENVENUE10")));
        var empty = new Cart();
        when(cartProvider.read(cartId)).thenReturn(Optional.of(empty));
        assertThrows(RuntimeException.class, () -> useCase().handle(new ApplyPromoCode.Command(cartId, "BIENVENUE10")));
        verify(cartProvider, never()).write(any());
    }

}
