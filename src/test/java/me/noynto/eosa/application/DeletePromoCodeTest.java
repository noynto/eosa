package me.noynto.eosa.application;

import me.noynto.eosa.checkout.Checkout;
import me.noynto.eosa.checkout.CheckoutItem;
import me.noynto.eosa.checkout.CheckoutSession;
import me.noynto.eosa.order.OrderState;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeletePromoCodeTest {

    @Mock PromoCodeProvider promoCodeProvider;

    private final InMemoryOrders orders = new InMemoryOrders();
    private final PromoCodeId id = new PromoCodeId("p-1");

    private void orderWithTheCode(OrderState state) {
        var session = new CheckoutSession();
        var sessionId = new CheckoutSessionId();
        sessionId.setStripe("cs_" + state);
        session.setId(sessionId);
        var item = new CheckoutItem();
        item.setName("Collier Lune");
        item.setUnitPrice(new BigDecimal("33.00"));
        item.setQuantity(1);
        var checkout = new Checkout();
        checkout.setSession(session);
        checkout.setItems(List.of(item));
        checkout.setDiscountAmount(new BigDecimal("3.30"));
        checkout.setPromoCodeId(id);
        checkout.setPromoCode("BIENVENUE10");
        var order = OrdersFromCheckouts.fromPaidCheckout(checkout, Instant.parse("2026-11-12T10:00:00Z"));
        order.setState(state);
        orders.write(order);
    }

    @Test
    void handle_deletesACodeThatNeverServed() {
        when(promoCodeProvider.read(id)).thenReturn(Optional.of(new PromoCode()));

        new DeletePromoCode(promoCodeProvider, orders).handle(new DeletePromoCode.Command(id));

        verify(promoCodeProvider).delete(id);
    }

    @Test
    void handle_refusesACodeUsedByAnOrderEvenCancelled() {
        when(promoCodeProvider.read(id)).thenReturn(Optional.of(new PromoCode()));
        orderWithTheCode(OrderState.CANCELLED);

        var error = assertThrows(RuntimeException.class,
                () -> new DeletePromoCode(promoCodeProvider, orders).handle(new DeletePromoCode.Command(id)));

        assertEquals("Ce code a déjà servi : désactivez-le plutôt que de le supprimer.", error.getMessage());
        verify(promoCodeProvider, never()).delete(id);
    }

    @Test
    void handle_throwsWhenIdIsNullOrCodeNotFound() {
        var useCase = new DeletePromoCode(promoCodeProvider, orders);
        assertThrows(RuntimeException.class, () -> useCase.handle(new DeletePromoCode.Command(null)));
        when(promoCodeProvider.read(id)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> useCase.handle(new DeletePromoCode.Command(id)));
        verify(promoCodeProvider, never()).delete(id);
    }

}
