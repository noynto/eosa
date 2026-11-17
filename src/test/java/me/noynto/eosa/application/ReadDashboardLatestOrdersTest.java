package me.noynto.eosa.application;

import me.noynto.eosa.order.Order;
import me.noynto.eosa.order.OrderState;
import me.noynto.eosa.shared.CheckoutSessionId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReadDashboardLatestOrdersTest {

    private final InMemoryOrders orders = new InMemoryOrders();

    private void order(int day) {
        var sessionId = new CheckoutSessionId();
        sessionId.setStripe("cs_" + day);
        var order = new Order();
        order.setStripeSessionId(sessionId);
        order.setPaidAt(Instant.parse("2026-11-%02dT10:00:00Z".formatted(day)));
        order.setState(OrderState.PAID);
        order.setItems(List.of());
        order.setItemsAmount(BigDecimal.ZERO);
        order.setShippingAmount(BigDecimal.ZERO);
        order.setTotalAmount(BigDecimal.ZERO);
        orders.write(order);
    }

    @Test
    void handle_returnsTheFiveMostRecentOrdersNewestFirst() {
        for (int day = 1; day <= 7; day++) {
            order(day);
        }

        var latest = new ReadDashboardLatestOrders(orders).handle().orders();

        assertEquals(5, latest.size());
        assertEquals(Instant.parse("2026-11-07T10:00:00Z"), latest.getFirst().getPaidAt());
        assertEquals(Instant.parse("2026-11-03T10:00:00Z"), latest.getLast().getPaidAt());
    }

    @Test
    void handle_returnsWhatThereIsWhenThereAreFewer() {
        order(1);

        assertEquals(1, new ReadDashboardLatestOrders(orders).handle().orders().size());
    }

}
