package me.noynto.eosa.application;

import me.noynto.eosa.cart.Cart;
import me.noynto.eosa.cart.CartItem;
import me.noynto.eosa.cart.CartProvider;
import me.noynto.eosa.cart.CartShippingRule;
import me.noynto.eosa.cart.CartShippingRuleProvider;
import me.noynto.eosa.giftcard.GiftCard;
import me.noynto.eosa.giftcard.GiftCardMovement;
import me.noynto.eosa.giftcard.GiftCardMovementKind;
import me.noynto.eosa.giftcard.GiftCardProvider;
import me.noynto.eosa.giftcard.GiftCardState;
import me.noynto.eosa.shared.CartId;
import me.noynto.eosa.shared.CartItemId;
import me.noynto.eosa.shared.GiftCardId;
import me.noynto.eosa.shared.JewelId;
import me.noynto.eosa.shared.PromoCodeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApplyGiftCardToCartTest {

    @Mock CartProvider cartProvider;
    @Mock GiftCardProvider giftCardProvider;
    @Mock CartShippingRuleProvider shippingRuleProvider;

    private final Instant now = Instant.parse("2026-11-16T10:00:00Z");
    private final CartId cartId = new CartId("cart");
    private final GiftCardId cardId = new GiftCardId("card-1");
    private Cart cart;

    @BeforeEach
    void setUp() {
        cart = new Cart();
        cart.setId(cartId);
        cart.setItems(List.of(new CartItem(new CartItemId("i"), new JewelId("j"), "Collier Lune", new BigDecimal("42"),
                null, 1, null, null, null, List.of())));
        when(cartProvider.read(cartId)).thenReturn(Optional.of(cart));
        when(cartProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));
        var rule = new CartShippingRule();
        rule.setFreeThreshold(new BigDecimal("60"));
        rule.setAmount(new BigDecimal("5"));
        when(shippingRuleProvider.get()).thenReturn(rule);

        var card = new GiftCard();
        card.setId(cardId);
        card.setState(GiftCardState.ACTIVE);
        card.setExpiresAt(now.plusSeconds(3600));
        card.getMovements().add(new GiftCardMovement(GiftCardMovementKind.ISSUED, new BigDecimal("30.00"), null, now));
        when(giftCardProvider.readIds(eq("ABCDEFGH2345"), isNull())).thenAnswer(inv -> Stream.of(cardId));
        when(giftCardProvider.read(cardId)).thenReturn(Optional.of(card));
    }

    private ApplyGiftCardToCart useCase() {
        return new ApplyGiftCardToCart(cartProvider, giftCardProvider, shippingRuleProvider, Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void handle_attachesTheCardAndRendersItsDeduction() {
        var result = useCase().handle(new ApplyGiftCardToCart.Command(cartId, " abcd-efgh 2345 "));

        assertEquals(cardId, cart.getGiftCardId());
        assertEquals(new BigDecimal("30.00"), result.deduction());
        verify(cartProvider).write(cart);
    }

    @Test
    void handle_refusesWhenTheCartCarriesAPromoCode() {
        cart.setPromoCodeId(new PromoCodeId("p-1"));

        var error = assertThrows(RuntimeException.class,
                () -> useCase().handle(new ApplyGiftCardToCart.Command(cartId, "ABCDEFGH2345")));

        assertEquals("Retirez d'abord le code de promotion pour utiliser une carte cadeau.", error.getMessage());
        verify(cartProvider, never()).write(any());
    }

    @Test
    void handle_refusesAnUnknownCodeWithTheGenericMessage() {
        var error = assertThrows(RuntimeException.class,
                () -> useCase().handle(new ApplyGiftCardToCart.Command(cartId, "ZZZZZZZZZZZZ")));

        assertEquals("Ce code n'est pas valable.", error.getMessage());
        verify(cartProvider, never()).write(any());
    }

    @Test
    void handle_refusesABlankCodeAnEmptyCartAndAMissingCart() {
        assertThrows(RuntimeException.class, () -> useCase().handle(new ApplyGiftCardToCart.Command(cartId, "  ")));
        assertThrows(RuntimeException.class, () -> useCase().handle(new ApplyGiftCardToCart.Command(null, "X")));
        cart.setItems(List.of());
        assertThrows(RuntimeException.class, () -> useCase().handle(new ApplyGiftCardToCart.Command(cartId, "ABCDEFGH2345")));
        when(cartProvider.read(cartId)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> useCase().handle(new ApplyGiftCardToCart.Command(cartId, "ABCDEFGH2345")));
        verify(cartProvider, never()).write(any());
    }

    @Test
    void removal_detachesTheCard() {
        cart.setGiftCardId(cardId);

        new RemoveGiftCardFromCart(cartProvider).handle(new RemoveGiftCardFromCart.Command(cartId));

        assertEquals(null, cart.getGiftCardId());
        verify(cartProvider).write(cart);
    }

}
