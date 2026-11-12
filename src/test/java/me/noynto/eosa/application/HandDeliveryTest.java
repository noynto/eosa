package me.noynto.eosa.application;

import me.noynto.eosa.mail.MailProvider;
import me.noynto.eosa.order.Order;
import me.noynto.eosa.order.OrderShippingAddress;
import me.noynto.eosa.order.OrderState;
import me.noynto.eosa.shared.CheckoutSessionId;
import me.noynto.eosa.shared.OrderId;
import me.noynto.eosa.shipping.ShippingLabelProvider;
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
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HandDeliveryTest {

    @Mock MailProvider mailProvider;
    @Mock ShippingLabelProvider shippingLabelProvider;

    private final Instant now = LocalDateTime.of(2026, 11, 18, 12, 0).atZone(me.noynto.eosa.service.BusinessTime.ZONE).toInstant();
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private InMemoryOrders orders;
    private Order order;

    @BeforeEach
    void setUp() {
        orders = new InMemoryOrders();
        var sessionId = new CheckoutSessionId();
        sessionId.setStripe("cs_1");
        order = new Order();
        order.setStripeSessionId(sessionId);
        order.setState(OrderState.PAID);
        order.setCustomerName("Ada Lovelace");
        order.setCustomerEmail("ada@example.com");
        order.setShippingAddress(new OrderShippingAddress("Ada Lovelace", "1 rue des Arts", null, "54000", "Nancy", "FR"));
        order.setItems(List.of());
        order.setItemsAmount(BigDecimal.ZERO);
        order.setShippingAmount(new BigDecimal("4.90"));
        order.setTotalAmount(new BigDecimal("4.90"));
        order.setPaidAt(now.minusSeconds(3600));
        orders.write(order);
    }

    @Test
    void craft_marksTheOrderCraftedForHandDeliveryWithoutLabelNorEmail() {
        craftForHand();

        assertEquals(OrderState.CRAFTED, order.getState());
        assertTrue(order.isHandDelivery());
        assertEquals(now, order.getCraftedAt());
        assertNull(order.getLabel());
        verify(mailProvider, never()).send(any());
        verify(shippingLabelProvider, never()).write(any());
    }

    @Test
    void craft_worksEvenWhenTheAddressWasErased() {
        order.setShippingAddress(null);

        craftForHand();

        assertEquals(OrderState.CRAFTED, order.getState());
    }

    @Test
    void craft_refusesAnOrderThatIsNotToCraft() {
        for (var state : List.of(OrderState.CRAFTED, OrderState.SHIPPED, OrderState.DELIVERED, OrderState.CANCELLED)) {
            order.setState(state);
            assertThrows(RuntimeException.class, this::craftForHand, state.name());
        }

        assertFalse(order.isHandDelivery());
    }

    @Test
    void craft_refusesAnUnknownOrder() {
        assertThrows(RuntimeException.class, () -> new CraftOrderForHandDelivery(orders, clock)
                .handle(new CraftOrderForHandDelivery.Command(new OrderId("unknown"))));
    }

    @Test
    void handOver_marksTheOrderDeliveredWithoutEmailAndWithoutShippingDate() {
        craftForHand();
        var later = Instant.parse("2026-11-20T09:00:00Z");

        new HandOverOrder(orders, Clock.fixed(later, ZoneOffset.UTC)).handle(new HandOverOrder.Command(order.getId()));

        assertEquals(OrderState.DELIVERED, order.getState());
        assertEquals(later, order.getDeliveredAt());
        assertNull(order.getShippedAt());
        verify(mailProvider, never()).send(any());
    }

    @Test
    void handOver_refusesAnOrderThatIsNotCraftedForHandDelivery() {
        // Fabriquée pour un envoi : seule l'expédition puis la livraison par transporteur s'appliquent.
        order.setState(OrderState.CRAFTED);
        assertThrows(RuntimeException.class, this::handOver);

        // À fabriquer, ou déjà livrée.
        order.setState(OrderState.PAID);
        order.setHandDelivery(true);
        assertThrows(RuntimeException.class, this::handOver);
        order.setState(OrderState.DELIVERED);
        assertThrows(RuntimeException.class, this::handOver);

        assertNull(order.getDeliveredAt());
    }

    @Test
    void ship_refusesAHandDeliveryOrderAndSendsNoEmail() {
        craftForHand();

        assertThrows(RuntimeException.class, () -> new ShipOrder(orders, mailProvider, clock)
                .handle(new ShipOrder.Command(order.getId())));

        assertEquals(OrderState.CRAFTED, order.getState());
        assertNull(order.getShippedAt());
        verify(mailProvider, never()).send(any());
    }

    @Test
    void craftForShipping_refusesAnOrderAlreadyChosenForHandDelivery() {
        order.setHandDelivery(true);

        assertThrows(RuntimeException.class, () -> new CraftOrder(orders, shippingLabelProvider, clock)
                .handle(new CraftOrder.Command(order.getId(), 150)));

        verify(shippingLabelProvider, never()).write(any());
    }

    @Test
    void aHandDeliveryOrderIsNeverLateForShippingButAnOrderToShipIs() {
        // Fabriquée il y a plusieurs jours : en retard d'expédition seulement si elle doit être expédiée.
        var craftedAt = LocalDateTime.of(2026, 11, 9, 10, 0).atZone(me.noynto.eosa.service.BusinessTime.ZONE).toInstant();
        order.setState(OrderState.CRAFTED);
        order.setCraftedAt(craftedAt);
        order.setHandDelivery(true);

        var hand = new ReadOrders(orders, clock).handle(new ReadOrders.Command(null, null)).orders().getFirst();
        var detail = new ReadOrder(orders, clock, 150).handle(new ReadOrder.Command(order.getId()));
        assertFalse(hand.overdue());
        assertFalse(detail.overdue());

        order.setHandDelivery(false);
        var shipping = new ReadOrders(orders, clock).handle(new ReadOrders.Command(null, null)).orders().getFirst();
        assertTrue(shipping.overdue());
        assertNotNull(order.getCraftedAt());
    }

    @Test
    void aHandDeliveredOrderIsPurgedAYearAfterTheHandOverLikeAnyDeliveredOrder() {
        craftForHand();
        new HandOverOrder(orders, clock).handle(new HandOverOrder.Command(order.getId()));

        var purge = new PurgeOrderPersonalData(orders, Clock.fixed(now.plusSeconds(366L * 86_400), ZoneOffset.UTC)).handle();

        assertEquals(1, purge.purged());
        assertNull(order.getShippingAddress());
    }

    private void craftForHand() {
        new CraftOrderForHandDelivery(orders, clock).handle(new CraftOrderForHandDelivery.Command(order.getId()));
    }

    private void handOver() {
        new HandOverOrder(orders, clock).handle(new HandOverOrder.Command(order.getId()));
    }

}
