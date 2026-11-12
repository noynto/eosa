package me.noynto.eosa.application;

import me.noynto.eosa.mail.Mail;
import me.noynto.eosa.mail.MailProvider;
import me.noynto.eosa.order.Order;
import me.noynto.eosa.order.OrderShippingAddress;
import me.noynto.eosa.order.OrderState;
import me.noynto.eosa.shared.CheckoutSessionId;
import me.noynto.eosa.shared.OrderId;
import me.noynto.eosa.shipping.ShippingCarrier;
import me.noynto.eosa.shipping.ShippingLabel;
import me.noynto.eosa.shipping.ShippingLabelStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ShipOrderTest {

    @Mock MailProvider mailProvider;

    private final Instant now = Instant.parse("2026-11-12T10:15:00Z");
    private InMemoryOrders orders;
    private Order order;

    @BeforeEach
    void setUp() {
        orders = new InMemoryOrders();
        var sessionId = new CheckoutSessionId();
        sessionId.setStripe("cs_1");
        order = new Order();
        order.setStripeSessionId(sessionId);
        order.setState(OrderState.CRAFTED);
        order.setCustomerName("Ada Lovelace");
        order.setCustomerEmail("ada@example.com");
        order.setShippingAddress(new OrderShippingAddress("Ada Lovelace", "1 rue des Arts", null, "54000", "Nancy", "FR"));
        order.setItems(List.of());
        order.setItemsAmount(BigDecimal.ZERO);
        order.setShippingAmount(BigDecimal.ZERO);
        order.setTotalAmount(BigDecimal.ZERO);
        order.setPaidAt(now.minusSeconds(86_400));
        var label = new ShippingLabel();
        label.setCarrier(ShippingCarrier.MONDIAL_RELAY);
        label.setTrackingNumber("00581108");
        label.setStatus(ShippingLabelStatus.CREATED);
        order.setLabel(label);
        orders.write(order);
    }

    @Test
    void handle_shipsTheCraftedOrderAndEmailsTheTrackingLink() {
        var result = ship();

        assertEquals(OrderState.SHIPPED, order.getState());
        assertEquals(now, order.getShippedAt());
        assertEquals(now, order.getShippingSentAt());
        assertTrue(result.emailSent());
        var mail = ArgumentCaptor.forClass(Mail.class);
        verify(mailProvider).send(mail.capture());
        assertEquals("ada@example.com", mail.getValue().to());
        assertEquals("order-shipped", mail.getValue().template());
        assertTrue(((String) mail.getValue().model().get("trackingUrl")).contains("NumeroExpedition=00581108"));
    }

    @Test
    void handle_shipsTheOrderEvenWhenTheEmailFailsAndLeavesItForTheRetry() {
        doThrow(new RuntimeException("SMTP indisponible")).when(mailProvider).send(any());

        var result = ship();

        assertEquals(OrderState.SHIPPED, order.getState());
        assertEquals(now, order.getShippedAt());
        assertNull(order.getShippingSentAt());
        assertFalse(result.emailSent());
    }

    @Test
    void handle_refusesAnOrderThatIsNotCrafted() {
        for (var state : List.of(OrderState.PAID, OrderState.SHIPPED, OrderState.DELIVERED, OrderState.CANCELLED)) {
            order.setState(state);
            assertThrows(RuntimeException.class, this::ship, state.name());
        }

        verify(mailProvider, never()).send(any());
        assertNull(order.getShippedAt());
    }

    @Test
    void handle_neverShipsTwiceNorSendsTheEmailTwice() {
        ship();

        assertThrows(RuntimeException.class, this::ship);

        verify(mailProvider, org.mockito.Mockito.times(1)).send(any());
    }

    @Test
    void handle_refusesAnUnknownOrder() {
        assertThrows(RuntimeException.class, () -> new ShipOrder(orders, mailProvider, Clock.fixed(now, ZoneOffset.UTC))
                .handle(new ShipOrder.Command(new OrderId("unknown"))));
    }

    private ShipOrder.Result ship() {
        return new ShipOrder(orders, mailProvider, Clock.fixed(now, ZoneOffset.UTC))
                .handle(new ShipOrder.Command(order.getId()));
    }

}
