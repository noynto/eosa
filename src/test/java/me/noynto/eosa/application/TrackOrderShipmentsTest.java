package me.noynto.eosa.application;

import me.noynto.eosa.mail.MailProvider;
import me.noynto.eosa.order.Order;
import me.noynto.eosa.order.OrderShippingAddress;
import me.noynto.eosa.order.OrderState;
import me.noynto.eosa.shared.CheckoutSessionId;
import me.noynto.eosa.shared.ShippingLabelId;
import me.noynto.eosa.shipping.ShippingCarrier;
import me.noynto.eosa.shipping.ShippingLabel;
import me.noynto.eosa.shipping.ShippingLabelProvider;
import me.noynto.eosa.shipping.ShippingLabelStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TrackOrderShipmentsTest {

    @Mock ShippingLabelProvider shippingLabelProvider;
    @Mock MailProvider mailProvider;

    private final Instant now = Instant.parse("2026-11-12T10:15:00Z");
    private final Instant deposit = Instant.parse("2026-11-10T16:35:00Z");
    private final Instant delivery = Instant.parse("2026-11-11T08:56:00Z");
    private final Map<String, ShippingLabel> carrier = new HashMap<>();
    private InMemoryOrders orders;

    @BeforeEach
    void setUp() {
        orders = new InMemoryOrders();
        when(shippingLabelProvider.read(any())).thenAnswer(inv -> {
            ShippingLabelId id = inv.getArgument(0);
            return Optional.ofNullable(carrier.get(id.value()));
        });
    }

    @Test
    void handle_shipsACraftedOrderOnceTheCarrierHasTheParcelWithTheCarrierDateAndEmailsTheCustomer() {
        var order = order("cs_1", "00000001", OrderState.CRAFTED);
        carrierSays("00000001", ShippingLabelStatus.DEPOSITED, deposit, null);

        var result = track();

        assertEquals(1, result.shipped());
        assertEquals(0, result.delivered());
        assertEquals(1, result.emailsSent());
        assertEquals(OrderState.SHIPPED, order.getState());
        assertEquals(deposit, order.getShippedAt());
        assertEquals(now, order.getShippingSentAt());
        assertEquals(ShippingLabelStatus.DEPOSITED, order.getLabel().getStatus());
        verify(mailProvider).send(any());
    }

    @Test
    void handle_marksAnOrderDeliveredWhenTheCarrierSaysSo() {
        var order = order("cs_1", "00000001", OrderState.SHIPPED);
        order.setShippedAt(deposit);
        order.setShippingSentAt(deposit);
        carrierSays("00000001", ShippingLabelStatus.DELIVERED, deposit, delivery);

        var result = track();

        assertEquals(0, result.shipped());
        assertEquals(1, result.delivered());
        assertEquals(OrderState.DELIVERED, order.getState());
        assertEquals(delivery, order.getDeliveredAt());
        assertEquals(deposit, order.getShippedAt());
        verify(mailProvider, never()).send(any());
    }

    @Test
    void handle_catchesUpAnOrderDeliveredWhileTheTaskWasDown() {
        var order = order("cs_1", "00000001", OrderState.CRAFTED);
        carrierSays("00000001", ShippingLabelStatus.DELIVERED, deposit, delivery);

        var result = track();

        assertEquals(1, result.shipped());
        assertEquals(1, result.delivered());
        assertEquals(OrderState.DELIVERED, order.getState());
        assertEquals(deposit, order.getShippedAt());
        assertEquals(delivery, order.getDeliveredAt());
        // La cliente reçoit quand même le lien de suivi.
        assertEquals(now, order.getShippingSentAt());
    }

    @Test
    void handle_leavesAnOrderAloneWhileTheParcelIsOnlyRecorded() {
        var order = order("cs_1", "00000001", OrderState.CRAFTED);
        carrierSays("00000001", ShippingLabelStatus.CREATED, null, null);

        var result = track();

        assertEquals(0, result.shipped());
        assertEquals(0, result.failed());
        assertEquals(OrderState.CRAFTED, order.getState());
        assertNull(order.getShippedAt());
        verify(mailProvider, never()).send(any());
    }

    @Test
    void handle_isIdempotentAndNeverEmailsTwice() {
        order("cs_1", "00000001", OrderState.CRAFTED);
        carrierSays("00000001", ShippingLabelStatus.DEPOSITED, deposit, null);

        track();
        var second = track();

        assertEquals(0, second.shipped());
        assertEquals(0, second.emailsSent());
        verify(mailProvider, times(1)).send(any());
    }

    @Test
    void handle_countsAnUnknownParcelAndStillTracksTheOthers() {
        order("cs_1", "00000001", OrderState.CRAFTED);
        var other = order("cs_2", "00000002", OrderState.CRAFTED);
        carrierSays("00000002", ShippingLabelStatus.DEPOSITED, deposit, null);

        var result = track();

        assertEquals(1, result.shipped());
        assertEquals(1, result.failed());
        assertEquals(OrderState.SHIPPED, other.getState());
    }

    @Test
    void handle_countsACarrierFailureAndKeepsTheOrder() {
        var order = order("cs_1", "00000001", OrderState.CRAFTED);
        doThrow(new RuntimeException("Mondial Relay injoignable")).when(shippingLabelProvider).read(any());

        var result = track();

        assertEquals(1, result.failed());
        assertEquals(OrderState.CRAFTED, order.getState());
    }

    @Test
    void handle_shipsTheOrderEvenWhenTheEmailFailsAndLeavesItForTheRetry() {
        var order = order("cs_1", "00000001", OrderState.CRAFTED);
        carrierSays("00000001", ShippingLabelStatus.DEPOSITED, deposit, null);
        doThrow(new RuntimeException("SMTP indisponible")).when(mailProvider).send(any());

        var result = track();

        assertEquals(1, result.shipped());
        assertEquals(0, result.emailsSent());
        assertEquals(1, result.failed());
        assertEquals(OrderState.SHIPPED, order.getState());
        assertNull(order.getShippingSentAt());
    }

    @Test
    void handle_neverDatesAStepInTheFuture() {
        var order = order("cs_1", "00000001", OrderState.CRAFTED);
        carrierSays("00000001", ShippingLabelStatus.DELIVERED, now.plus(Duration.ofHours(3)), now.plus(Duration.ofDays(1)));

        track();

        assertEquals(now, order.getShippedAt());
        assertEquals(now, order.getDeliveredAt());
    }

    @Test
    void handle_ignoresOrdersThatAreNotInTransit() {
        for (var state : List.of(OrderState.PAID, OrderState.DELIVERED, OrderState.CANCELLED)) {
            order("cs_" + state, "00000009", state);
        }
        carrierSays("00000009", ShippingLabelStatus.DELIVERED, deposit, delivery);

        var result = track();

        assertEquals(0, result.shipped() + result.delivered() + result.failed());
        verify(shippingLabelProvider, never()).read(any());
    }

    private TrackOrderShipments.Result track() {
        return new TrackOrderShipments(orders, shippingLabelProvider, mailProvider, Clock.fixed(now, ZoneOffset.UTC)).handle();
    }

    private void carrierSays(String trackingNumber, ShippingLabelStatus status, Instant depositedAt, Instant deliveredAt) {
        var label = new ShippingLabel();
        label.setCarrier(ShippingCarrier.MONDIAL_RELAY);
        label.setTrackingNumber(trackingNumber);
        label.setStatus(status);
        label.setDepositedAt(depositedAt);
        label.setDeliveredAt(deliveredAt);
        carrier.put(trackingNumber, label);
    }

    private Order order(String stripeSession, String trackingNumber, OrderState state) {
        var sessionId = new CheckoutSessionId();
        sessionId.setStripe(stripeSession);
        var order = new Order();
        order.setStripeSessionId(sessionId);
        order.setState(state);
        order.setCustomerName("Ada Lovelace");
        order.setCustomerEmail("ada@example.com");
        order.setShippingAddress(new OrderShippingAddress("Ada Lovelace", "1 rue des Arts", null, "54000", "Nancy", "FR"));
        order.setItems(List.of());
        order.setItemsAmount(BigDecimal.ZERO);
        order.setShippingAmount(BigDecimal.ZERO);
        order.setTotalAmount(BigDecimal.ZERO);
        order.setPaidAt(now.minus(Duration.ofDays(5)));
        var label = new ShippingLabel();
        label.setCarrier(ShippingCarrier.MONDIAL_RELAY);
        label.setTrackingNumber(trackingNumber);
        label.setStatus(ShippingLabelStatus.CREATED);
        order.setLabel(label);
        return orders.write(order);
    }

}
