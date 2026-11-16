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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InitiateCheckoutTest {

    @Mock CartProvider cartProvider;
    @Mock CheckoutProvider checkoutProvider;
    @Mock CartShippingRuleProvider shippingRuleProvider;
    @Mock PromoCodeProvider promoCodeProvider;
    @Mock me.noynto.eosa.giftcard.GiftCardProvider giftCardProvider;

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
        org.mockito.Mockito.lenient().when(checkoutProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Checkout initiate() throws Exception {
        var useCase = new InitiateCheckout(cartProvider, checkoutProvider, shippingRuleProvider, promoCodeProvider,
                new InMemoryOrders(), giftCardProvider, Clock.fixed(now, ZoneOffset.UTC));
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

    private me.noynto.eosa.giftcard.GiftCard giftCard(String balance) {
        var giftCard = new me.noynto.eosa.giftcard.GiftCard();
        giftCard.setId(new me.noynto.eosa.shared.GiftCardId("card-1"));
        giftCard.setState(me.noynto.eosa.giftcard.GiftCardState.ACTIVE);
        giftCard.setExpiresAt(now.plusSeconds(3600));
        giftCard.getMovements().add(new me.noynto.eosa.giftcard.GiftCardMovement(
                me.noynto.eosa.giftcard.GiftCardMovementKind.ISSUED, new BigDecimal(balance), null, now));
        // Chaque écriture donne leur identifiant aux mouvements neufs, comme le fait la persistance.
        org.mockito.Mockito.lenient().when(giftCardProvider.write(any())).thenAnswer(inv -> {
            me.noynto.eosa.giftcard.GiftCard written = inv.getArgument(0);
            written.getMovements().forEach(movement -> {
                if (movement.getId() == null) {
                    movement.setId(new me.noynto.eosa.shared.GiftCardMovementId("m-" + written.getMovements().indexOf(movement)));
                }
            });
            return written;
        });
        org.mockito.Mockito.lenient().when(giftCardProvider.read(giftCard.getId())).thenReturn(Optional.of(giftCard));
        cart.setGiftCardId(giftCard.getId());
        return giftCard;
    }

    @Test
    void handle_reservesTheCardAndAsksStripeForItsPartWithAnExpiry() throws Exception {
        var giftCard = giftCard("20");
        when(checkoutProvider.write(any())).thenAnswer(inv -> {
            Checkout opened = inv.getArgument(0);
            var session = new me.noynto.eosa.checkout.CheckoutSession();
            var sessionId = new me.noynto.eosa.shared.CheckoutSessionId();
            sessionId.setStripe("cs_1");
            session.setId(sessionId);
            opened.setSession(session);
            return opened;
        });

        var checkout = initiate();

        assertEquals(new BigDecimal("20.00"), checkout.getUsedGiftCardAmount());
        assertEquals(giftCard.getId(), checkout.getUsedGiftCardId());
        assertNull(checkout.getDiscountAmount(), "une carte n'est pas une remise de code");
        assertEquals(now.plus(PurchaseGiftCard.SESSION_DURATION), checkout.getExpiresAt());
        // Le solde est réservé, et la réservation suit la session.
        assertEquals(0, BigDecimal.ZERO.compareTo(giftCard.balance()));
        assertEquals(1, giftCard.openReservations().size());
        assertEquals("cs_1", giftCard.openReservations().getFirst().getCheckoutSessionId());
    }

    @Test
    void handle_opensThePaymentWithoutTheCardWhenItNoLongerApplies() throws Exception {
        var giftCard = giftCard("20");
        giftCard.setExpiresAt(now);

        var checkout = initiate();

        assertNull(checkout.getUsedGiftCardId());
        assertNull(checkout.getUsedGiftCardAmount());
        assertEquals(1, giftCard.getMovements().size(), "rien n'est réservé");
    }

    @Test
    void handle_releasesTheReservationWhenStripeRefuses() {
        var giftCard = giftCard("20");
        org.mockito.Mockito.doThrow(new RuntimeException("Stripe")).when(checkoutProvider).write(any());
        var useCase = new InitiateCheckout(cartProvider, checkoutProvider, shippingRuleProvider, promoCodeProvider,
                new InMemoryOrders(), giftCardProvider, Clock.fixed(now, ZoneOffset.UTC));

        assertThrows(RuntimeException.class, () -> useCase.handle(new InitiateCheckout.Command(cartId)));

        assertEquals(0, new BigDecimal("20").compareTo(giftCard.balance()), "le solde est rendu");
    }

    @Test
    void handle_ignoresTheCardWhenAPromoCodeIsAlsoAttached() throws Exception {
        var giftCard = giftCard("20");
        cart.setPromoCodeId(promoCodeId);
        when(promoCodeProvider.read(promoCodeId)).thenReturn(Optional.empty());

        var checkout = initiate();

        assertNull(checkout.getUsedGiftCardId());
        assertEquals(1, giftCard.getMovements().size());
    }

    @Test
    void handle_sendsNoDiscountWithoutACode() throws Exception {
        var checkout = initiate();

        assertNull(checkout.getDiscountAmount());
        assertNull(checkout.getPromoCodeId());
    }

}
