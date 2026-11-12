package me.noynto.eosa.application;

import me.noynto.eosa.order.Order;
import me.noynto.eosa.order.OrderState;
import me.noynto.eosa.shared.CheckoutSessionId;
import me.noynto.eosa.shared.OrderId;
import me.noynto.eosa.shipping.ShippingCarrier;
import me.noynto.eosa.shipping.ShippingLabel;
import me.noynto.eosa.shipping.ShippingLabelStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReadOrderLabelTest {

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
        order.setItems(List.of());
        order.setItemsAmount(BigDecimal.ZERO);
        order.setShippingAmount(BigDecimal.ZERO);
        order.setTotalAmount(BigDecimal.ZERO);
        order.setPaidAt(Instant.parse("2026-11-12T08:00:00Z"));
        var label = new ShippingLabel();
        label.setCarrier(ShippingCarrier.MONDIAL_RELAY);
        label.setTrackingNumber("96408887");
        label.setReference("https://labels.example/96408887.pdf");
        label.setStatus(ShippingLabelStatus.CREATED);
        order.setLabel(label);
        orders.write(order);
    }

    @Test
    void handle_givesTheLabelAddressOfACraftedOrderAndOfAShippedOne() {
        assertEquals("https://labels.example/96408887.pdf", read().url());

        order.setState(OrderState.SHIPPED);

        assertEquals("https://labels.example/96408887.pdf", read().url());
    }

    @Test
    void handle_refusesAnOrderWithoutLabelOrNoLongerToShip() {
        order.setState(OrderState.PAID);
        order.setLabel(null);
        assertThrows(RuntimeException.class, this::read);

        order.setState(OrderState.CRAFTED);
        assertThrows(RuntimeException.class, this::read);
    }

    @Test
    void handle_refusesAnUnknownOrder() {
        assertThrows(RuntimeException.class, () -> new ReadOrderLabel(orders)
                .handle(new ReadOrderLabel.Command(new OrderId("unknown"))));
    }

    private ReadOrderLabel.Result read() {
        return new ReadOrderLabel(orders).handle(new ReadOrderLabel.Command(order.getId()));
    }

}
