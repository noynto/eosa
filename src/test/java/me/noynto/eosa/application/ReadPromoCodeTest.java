package me.noynto.eosa.application;

import me.noynto.eosa.checkout.Checkout;
import me.noynto.eosa.checkout.CheckoutItem;
import me.noynto.eosa.checkout.CheckoutSession;
import me.noynto.eosa.promo.PromoCode;
import me.noynto.eosa.promo.PromoCodeProvider;
import me.noynto.eosa.service.OrdersFromCheckouts;
import me.noynto.eosa.shared.CheckoutSessionId;
import me.noynto.eosa.shared.PromoCodeId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadPromoCodeTest {

    @Mock PromoCodeProvider promoCodeProvider;

    private final InMemoryOrders orders = new InMemoryOrders();
    private final PromoCodeId id = new PromoCodeId("p-1");
    private final PromoCodeId other = new PromoCodeId("p-2");

    private void order(String session, PromoCodeId promoCodeId, String discount) {
        var checkoutSession = new CheckoutSession();
        var sessionId = new CheckoutSessionId();
        sessionId.setStripe(session);
        checkoutSession.setId(sessionId);
        var item = new CheckoutItem();
        item.setName("Collier Lune");
        item.setUnitPrice(new BigDecimal("33.00"));
        item.setQuantity(1);
        var checkout = new Checkout();
        checkout.setSession(checkoutSession);
        checkout.setItems(List.of(item));
        if (promoCodeId != null) {
            checkout.setDiscountAmount(new BigDecimal(discount));
            checkout.setPromoCodeId(promoCodeId);
            checkout.setPromoCode("CODE");
        }
        orders.write(OrdersFromCheckouts.fromPaidCheckout(checkout, Instant.parse("2026-11-12T10:00:00Z")));
    }

    @Test
    void readOne_listsOnlyTheOrdersOfTheCodeAndAddsUpTheDiscount() {
        var promoCode = new PromoCode();
        promoCode.setId(id);
        when(promoCodeProvider.read(id)).thenReturn(Optional.of(promoCode));
        order("cs_1", id, "3.30");
        order("cs_2", id, "1.70");
        order("cs_3", other, "9.00");
        order("cs_4", null, "0");

        var result = new ReadPromoCode(promoCodeProvider, orders).handle(new ReadPromoCode.Command(id));

        assertEquals(2, result.orders().size());
        assertEquals(new BigDecimal("5.00"), result.totalDiscount());
    }

    @Test
    void readOne_throwsWhenIdIsNullOrCodeNotFound() {
        var useCase = new ReadPromoCode(promoCodeProvider, orders);
        assertThrows(RuntimeException.class, () -> useCase.handle(new ReadPromoCode.Command(null)));
        when(promoCodeProvider.read(id)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> useCase.handle(new ReadPromoCode.Command(id)));
    }

    @Test
    void readAll_countsTheUsesOfEachCode() {
        var first = new PromoCode();
        first.setId(id);
        var second = new PromoCode();
        second.setId(other);
        when(promoCodeProvider.readIds(null)).thenReturn(Stream.of(id, other));
        when(promoCodeProvider.read(id)).thenReturn(Optional.of(first));
        when(promoCodeProvider.read(other)).thenReturn(Optional.of(second));
        order("cs_1", id, "3.30");
        order("cs_2", id, "1.70");

        var entries = new ReadPromoCodes(promoCodeProvider, orders).handle().entries();

        assertEquals(2, entries.get(0).uses());
        assertEquals(0, entries.get(1).uses());
    }

}
