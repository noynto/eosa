package me.noynto.eosa.application;

import me.noynto.eosa.order.Order;
import me.noynto.eosa.order.OrderState;
import me.noynto.eosa.service.BusinessTime;
import me.noynto.eosa.shared.CheckoutSessionId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReadDashboardMonthTest {

    // Mardi 18 novembre 2026, 12 h à Paris.
    private final Instant now = at(2026, 11, 18, 12);
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private InMemoryOrders orders;

    @BeforeEach
    void setUp() {
        orders = new InMemoryOrders();
    }

    private static Instant at(int year, int month, int day, int hour) {
        return LocalDateTime.of(year, month, day, hour, 0).atZone(BusinessTime.ZONE).toInstant();
    }

    private Order order(String session, Instant paidAt, OrderState state, String items, String shipping,
                        String total, String discount, String giftCard) {
        var sessionId = new CheckoutSessionId();
        sessionId.setStripe(session);
        var order = new Order();
        order.setStripeSessionId(sessionId);
        order.setPaidAt(paidAt);
        order.setState(state);
        order.setItems(List.of());
        order.setItemsAmount(new BigDecimal(items));
        order.setShippingAmount(new BigDecimal(shipping));
        order.setTotalAmount(new BigDecimal(total));
        order.setDiscountAmount(new BigDecimal(discount));
        order.setGiftCardAmount(new BigDecimal(giftCard));
        return orders.write(order);
    }

    @Test
    void handle_sumsTheMonthIncludingShippingAndTheGiftCardPartAndLeavesCancelledOnesOut() {
        // 66 € de bijoux + 4,90 € de livraison, payés par carte bancaire : 70,90 €.
        order("cs_1", at(2026, 11, 3, 10), OrderState.PAID, "66.00", "4.90", "70.90", "0", "0");
        // 42 € de bijoux, 20 € par carte cadeau, 26,90 € par carte bancaire.
        order("cs_2", at(2026, 11, 10, 10), OrderState.SHIPPED, "42.00", "4.90", "26.90", "0", "20.00");
        // Code de promotion : 40 € de bijoux, 4 € de remise.
        order("cs_3", at(2026, 11, 12, 10), OrderState.DELIVERED, "40.00", "4.90", "40.90", "4.00", "0");
        // Annulée : exclue.
        order("cs_4", at(2026, 11, 14, 10), OrderState.CANCELLED, "100.00", "4.90", "104.90", "0", "0");

        var month = new ReadDashboardMonth(orders, clock).handle();

        assertEquals(LocalDate.of(2026, 11, 1), month.firstDay());
        assertEquals(3, month.orders());
        assertEquals(new BigDecimal("158.70"), month.revenue());
        assertEquals(new BigDecimal("14.70"), month.shipping());
        assertEquals(new BigDecimal("52.90"), month.averageBasket());
        assertEquals(new BigDecimal("49.33"), month.averageItems());
        assertEquals(new BigDecimal("4.00"), month.promoDiscount());
        assertEquals(new BigDecimal("20.00"), month.giftCardPart());
    }

    @Test
    void handle_countsThePreviousMonthOnlyToCompare() {
        order("cs_1", at(2026, 10, 20, 10), OrderState.DELIVERED, "30.00", "4.90", "34.90", "0", "0");
        order("cs_2", at(2026, 10, 31, 23), OrderState.DELIVERED, "30.00", "4.90", "34.90", "0", "0");
        order("cs_3", at(2026, 11, 1, 0), OrderState.PAID, "30.00", "4.90", "34.90", "0", "0");
        // Deux mois plus tôt : hors comparaison.
        order("cs_4", at(2026, 9, 20, 10), OrderState.DELIVERED, "30.00", "4.90", "34.90", "0", "0");

        var month = new ReadDashboardMonth(orders, clock).handle();

        assertEquals(1, month.orders());
        assertEquals(2, month.previousOrders());
    }

    @Test
    void handle_isZeroWithoutOrdersAndDoesNotDivideByZero() {
        var month = new ReadDashboardMonth(orders, clock).handle();

        assertEquals(0, month.orders());
        assertEquals(0, BigDecimal.ZERO.compareTo(month.revenue()));
        assertEquals(0, BigDecimal.ZERO.compareTo(month.averageBasket()));
        assertEquals(0, BigDecimal.ZERO.compareTo(month.averageItems()));
    }

}
