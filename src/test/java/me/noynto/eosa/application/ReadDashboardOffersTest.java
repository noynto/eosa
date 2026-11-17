package me.noynto.eosa.application;

import me.noynto.eosa.giftcard.GiftCard;
import me.noynto.eosa.giftcard.GiftCardMovement;
import me.noynto.eosa.giftcard.GiftCardMovementKind;
import me.noynto.eosa.giftcard.GiftCardProvider;
import me.noynto.eosa.giftcard.GiftCardState;
import me.noynto.eosa.jewel.Jewel;
import me.noynto.eosa.jewel.JewelCategory;
import me.noynto.eosa.jewel.JewelProvider;
import me.noynto.eosa.jewel.JewelState;
import me.noynto.eosa.order.Order;
import me.noynto.eosa.order.OrderState;
import me.noynto.eosa.promo.PromoCode;
import me.noynto.eosa.promo.PromoCodeProvider;
import me.noynto.eosa.shared.CheckoutSessionId;
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
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReadDashboardOffersTest {

    @Mock GiftCardProvider giftCardProvider;
    @Mock PromoCodeProvider promoCodeProvider;
    @Mock JewelProvider jewelProvider;

    private final Instant now = Instant.parse("2026-11-18T11:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private InMemoryOrders orders;

    @BeforeEach
    void setUp() {
        orders = new InMemoryOrders();
        when(giftCardProvider.readIds(any(), any())).thenAnswer(inv -> Stream.empty());
        when(promoCodeProvider.readIds(any())).thenAnswer(inv -> Stream.empty());
        when(jewelProvider.readIds(any(), any())).thenAnswer(inv -> Stream.empty());
    }

    private ReadDashboardOffers.Result read() {
        return new ReadDashboardOffers(giftCardProvider, promoCodeProvider, orders, jewelProvider, clock).handle();
    }

    private GiftCard card(String id, GiftCardState state, String balance, Instant createdAt, Instant expiresAt) {
        var giftCard = new GiftCard();
        giftCard.setId(new GiftCardId(id));
        giftCard.setState(state);
        giftCard.setCreatedAt(createdAt);
        giftCard.setExpiresAt(expiresAt);
        giftCard.getMovements().add(new GiftCardMovement(GiftCardMovementKind.ISSUED, new BigDecimal(balance), null, createdAt));
        when(giftCardProvider.read(giftCard.getId())).thenReturn(Optional.of(giftCard));
        return giftCard;
    }

    @Test
    void giftCards_sumWhatIsStillToHonourFromUsableCardsOnly() {
        var a = card("a", GiftCardState.ACTIVE, "50.00", Instant.parse("2026-11-02T10:00:00Z"), Instant.parse("2027-11-03T23:00:00Z"));
        var b = card("b", GiftCardState.ACTIVE, "100.00", Instant.parse("2026-10-10T10:00:00Z"), Instant.parse("2027-01-03T23:00:00Z"));
        var expired = card("expired", GiftCardState.ACTIVE, "30.00", Instant.parse("2025-01-01T10:00:00Z"), Instant.parse("2026-01-01T23:00:00Z"));
        var voided = card("void", GiftCardState.VOID, "20.00", Instant.parse("2026-11-05T10:00:00Z"), Instant.parse("2027-11-05T23:00:00Z"));
        var pending = card("pending", GiftCardState.PENDING_PAYMENT, "10.00", Instant.parse("2026-11-17T10:00:00Z"), Instant.parse("2027-11-17T23:00:00Z"));
        when(giftCardProvider.readIds(any(), any())).thenAnswer(inv ->
                Stream.of(a, b, expired, voided, pending).map(GiftCard::getId));

        var giftCards = read().giftCards();

        assertEquals(2, giftCards.usable());
        assertEquals(new BigDecimal("150.00"), giftCards.toHonour());
        // Émises en novembre : a et void (pending exclue : jamais payée).
        assertEquals(2, giftCards.issuedThisMonth());
        assertEquals(Instant.parse("2027-01-03T23:00:00Z"), giftCards.nextExpiry());
    }

    @Test
    void promoCodes_countCodesInForceUsesOfTheMonthAndTheTopCode() {
        var welcome = promoCode("p-1", "BIENVENUE10", true, null, Instant.parse("2026-12-01T00:00:00Z"), 20);
        var spring = promoCode("p-2", "PRINTEMPS", true, Instant.parse("2027-03-01T00:00:00Z"), null, null);
        var off = promoCode("p-3", "ARRET", false, null, null, null);
        var expired = promoCode("p-4", "VIEUX", true, null, Instant.parse("2026-10-01T00:00:00Z"), null);
        when(promoCodeProvider.readIds(any())).thenAnswer(inv -> Stream.of(welcome, spring, off, expired).map(PromoCode::getId));

        order("cs_1", Instant.parse("2026-11-03T10:00:00Z"), OrderState.PAID, welcome.getId(), "4.00");
        order("cs_2", Instant.parse("2026-11-05T10:00:00Z"), OrderState.DELIVERED, welcome.getId(), "3.00");
        order("cs_3", Instant.parse("2026-11-06T10:00:00Z"), OrderState.CANCELLED, welcome.getId(), "5.00");
        order("cs_4", Instant.parse("2026-11-07T10:00:00Z"), OrderState.PAID, expired.getId(), "2.00");
        // Un usage d'octobre : compte dans les usages du code, pas dans ceux du mois.
        order("cs_5", Instant.parse("2026-10-20T10:00:00Z"), OrderState.PAID, welcome.getId(), "1.00");

        var promoCodes = read().promoCodes();

        assertEquals(1, promoCodes.inForce(), "BIENVENUE10 ; PRINTEMPS n'a pas commencé, ARRET est désactivé, VIEUX a expiré");
        assertEquals(4, promoCodes.usesThisMonth());
        assertEquals(new BigDecimal("9.00"), promoCodes.discountThisMonth(), "la commande annulée n'est pas comptée");
        assertEquals("BIENVENUE10", promoCodes.top().code());
        assertEquals(4, promoCodes.top().uses());
        assertEquals(20, promoCodes.top().maxUses());
    }

    @Test
    void promoCodes_haveNoTopCodeWithoutUseThisMonth() {
        assertNull(read().promoCodes().top());
        assertEquals(0, read().promoCodes().usesThisMonth());
    }

    @Test
    void catalogue_countsJewelsByStateAndCategoryAndFindsTheLastPublication() {
        var necklace = jewel("n", JewelState.PUBLISHED, JewelCategory.NECKLACE, Instant.parse("2026-11-12T10:00:00Z"));
        var older = jewel("o", JewelState.PUBLISHED, JewelCategory.NECKLACE, Instant.parse("2026-10-01T10:00:00Z"));
        var bracelet = jewel("b", JewelState.PUBLISHED, JewelCategory.BRACELET, Instant.parse("2026-11-01T10:00:00Z"));
        var draft = jewel("d", JewelState.DRAFTED, JewelCategory.NECKLACE, null);
        var archived = jewel("a", JewelState.ARCHIVED, JewelCategory.BRACELET, Instant.parse("2026-01-01T10:00:00Z"));
        when(jewelProvider.readIds(any(), any())).thenAnswer(inv -> Stream.of(necklace, older, bracelet, draft, archived).map(Jewel::getId));

        var catalogue = read().catalogue();

        assertEquals(3, catalogue.published());
        assertEquals(2, catalogue.necklaces());
        assertEquals(1, catalogue.bracelets());
        assertEquals(1, catalogue.drafted());
        assertEquals(1, catalogue.archived());
        assertEquals(Instant.parse("2026-11-12T10:00:00Z"), catalogue.lastPublishedAt());
    }

    private PromoCode promoCode(String id, String code, boolean active, Instant startsAt, Instant endsAt, Integer maxUses) {
        var promoCode = new PromoCode();
        promoCode.setId(new PromoCodeId(id));
        promoCode.setCode(code);
        promoCode.setActive(active);
        promoCode.setStartsAt(startsAt);
        promoCode.setEndsAt(endsAt);
        promoCode.setMaxUses(maxUses);
        when(promoCodeProvider.read(promoCode.getId())).thenReturn(Optional.of(promoCode));
        return promoCode;
    }

    private void order(String session, Instant paidAt, OrderState state, PromoCodeId promoCodeId, String discount) {
        var sessionId = new CheckoutSessionId();
        sessionId.setStripe(session);
        var order = new Order();
        order.setStripeSessionId(sessionId);
        order.setPaidAt(paidAt);
        order.setState(state);
        order.setItems(List.of());
        order.setItemsAmount(BigDecimal.ZERO);
        order.setShippingAmount(BigDecimal.ZERO);
        order.setTotalAmount(BigDecimal.ZERO);
        order.setPromoCodeId(promoCodeId);
        order.setDiscountAmount(new BigDecimal(discount));
        orders.write(order);
    }

    private Jewel jewel(String id, JewelState state, JewelCategory category, Instant publishedAt) {
        var jewel = new Jewel();
        jewel.setId(new JewelId(id));
        jewel.setState(state);
        jewel.setCategory(category);
        jewel.setPublishedAt(publishedAt);
        when(jewelProvider.read(jewel.getId())).thenReturn(Optional.of(jewel));
        return jewel;
    }

}
