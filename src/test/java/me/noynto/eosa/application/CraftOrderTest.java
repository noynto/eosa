package me.noynto.eosa.application;

import me.noynto.eosa.order.Order;
import me.noynto.eosa.order.OrderShippingAddress;
import me.noynto.eosa.order.OrderState;
import me.noynto.eosa.shared.CheckoutSessionId;
import me.noynto.eosa.shared.OrderId;
import me.noynto.eosa.shipping.ShippingCarrier;
import me.noynto.eosa.shipping.ShippingLabel;
import me.noynto.eosa.shipping.ShippingLabelProvider;
import me.noynto.eosa.shipping.ShippingLabelStatus;
import me.noynto.eosa.shipping.ShippingRecipient;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CraftOrderTest {

    @Mock ShippingLabelProvider shippingLabelProvider;

    private final Instant now = Instant.parse("2026-11-12T10:15:00Z");
    private InMemoryOrders orders;
    private Order order;
    private ShippingRecipient sentRecipient;

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
        order.setCustomerPhone("+33600000000");
        order.setShippingAddress(new OrderShippingAddress("Ada Lovelace", "1 rue des Arts", "Bât. B", "54000", "Nancy", "FR"));
        order.setItems(List.of());
        order.setItemsAmount(BigDecimal.ZERO);
        order.setShippingAmount(BigDecimal.ZERO);
        order.setTotalAmount(BigDecimal.ZERO);
        order.setPaidAt(now.minusSeconds(3600));
        orders.write(order);
        when(shippingLabelProvider.write(any())).thenAnswer(inv -> {
            ShippingLabel label = inv.getArgument(0);
            // Le destinataire est retiré de l'étiquette une fois écrite : on le garde tel qu'il est envoyé.
            sentRecipient = label.getRecipient();
            label.setCarrier(ShippingCarrier.MONDIAL_RELAY);
            label.setTrackingNumber("96408887");
            label.setReference("https://labels.example/96408887.pdf");
            label.setStatus(ShippingLabelStatus.CREATED);
            return label;
        });
    }

    @Test
    void handle_createsTheLabelAndMarksTheOrderCrafted() {
        var result = craft(250);

        assertEquals("96408887", result.trackingNumber());
        assertEquals(OrderState.CRAFTED, order.getState());
        assertEquals(now, order.getCraftedAt());
        var label = order.getLabel();
        assertEquals("96408887", label.getTrackingNumber());
        assertEquals(250, label.getWeightGrams());
        assertEquals(ShippingLabelStatus.CREATED, label.getStatus());
        assertEquals("https://labels.example/96408887.pdf", label.getReference());
        assertEquals(now, label.getCreatedAt());
        // Le destinataire n'est pas conservé avec l'étiquette : l'adresse reste dans la commande.
        assertNull(label.getRecipient());
    }

    @Test
    void handle_sendsTheRecipientAndTheWeightToTheCarrier() {
        craft(250);

        var sent = ArgumentCaptor.forClass(ShippingLabel.class);
        verify(shippingLabelProvider).write(sent.capture());
        assertEquals("EOSA-2026-0001", sent.getValue().getOrderNumber());
        assertEquals(250, sent.getValue().getWeightGrams());
        assertEquals(new ShippingRecipient("Ada Lovelace", "ada@example.com", "+33600000000",
                "1 rue des Arts", "Bât. B", "54000", "Nancy", "FR"), sentRecipient);
    }

    @Test
    void handle_keepsTheOrderToCraftWhenTheCarrierRefuses() {
        doThrow(new RuntimeException("Mondial Relay a refusé l'étiquette")).when(shippingLabelProvider).write(any());

        assertThrows(RuntimeException.class, () -> craft(250));

        assertEquals(OrderState.PAID, order.getState());
        assertNull(order.getLabel());
        assertNull(order.getCraftedAt());
    }

    @Test
    void handle_createsOnlyOneLabelPerOrder() {
        craft(250);

        assertThrows(RuntimeException.class, () -> craft(250));

        verify(shippingLabelProvider, org.mockito.Mockito.times(1)).write(any());
    }

    @Test
    void handle_refusesAnOrderThatIsNotToCraft() {
        order.setState(OrderState.CANCELLED);

        assertThrows(RuntimeException.class, () -> craft(250));

        verify(shippingLabelProvider, never()).write(any());
    }

    @Test
    void handle_refusesAnOrderWhoseAddressWasErased() {
        order.setShippingAddress(null);

        assertThrows(RuntimeException.class, () -> craft(250));

        verify(shippingLabelProvider, never()).write(any());
    }

    @Test
    void handle_refusesAWeightThatIsNotPositive() {
        assertThrows(RuntimeException.class, () -> craft(0));
        assertThrows(RuntimeException.class, () -> craft(-5));

        verify(shippingLabelProvider, never()).write(any());
    }

    @Test
    void handle_refusesAnUnknownOrder() {
        assertThrows(RuntimeException.class, () -> new CraftOrder(orders, shippingLabelProvider, Clock.fixed(now, ZoneOffset.UTC))
                .handle(new CraftOrder.Command(new OrderId("unknown"), 250)));
    }

    private CraftOrder.Result craft(int weightGrams) {
        return new CraftOrder(orders, shippingLabelProvider, Clock.fixed(now, ZoneOffset.UTC))
                .handle(new CraftOrder.Command(order.getId(), weightGrams));
    }

}
