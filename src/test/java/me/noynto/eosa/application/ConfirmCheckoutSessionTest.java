package me.noynto.eosa.application;

import me.noynto.eosa.cart.Cart;
import me.noynto.eosa.cart.CartProvider;
import me.noynto.eosa.checkout.Checkout;
import me.noynto.eosa.checkout.CheckoutItem;
import me.noynto.eosa.checkout.CheckoutProvider;
import me.noynto.eosa.checkout.CheckoutSession;
import me.noynto.eosa.checkout.CheckoutShippingAddress;
import me.noynto.eosa.checkout.CheckoutStatus;
import me.noynto.eosa.mail.Mail;
import me.noynto.eosa.mail.MailProvider;
import me.noynto.eosa.order.OrderState;
import me.noynto.eosa.shared.CartId;
import me.noynto.eosa.shared.CheckoutId;
import me.noynto.eosa.shared.CheckoutSessionId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConfirmCheckoutSessionTest {

    @Mock CheckoutProvider checkoutProvider;
    @Mock CartProvider cartProvider;
    @Mock MailProvider mailProvider;

    private final Instant now = Instant.parse("2026-11-12T10:15:00Z");
    private final CartId cartId = new CartId("cart");
    private InMemoryOrders orders;
    private Cart cart;
    private Checkout checkout;
    private CheckoutSessionId sessionId;

    @BeforeEach
    void setUp() {
        orders = new InMemoryOrders();
        sessionId = new CheckoutSessionId();
        sessionId.setStripe("cs_test_1");
        var checkoutId = new CheckoutId();
        checkoutId.setStripe("cs_test_1");

        checkout = new Checkout();
        checkout.setId(checkoutId);
        checkout.setCartId(cartId);
        checkout.setStatus(CheckoutStatus.COMPLETED);
        var session = new CheckoutSession();
        session.setId(sessionId);
        checkout.setSession(session);
        checkout.setItems(List.of(item("Collier Lune", "33.00", 2)));
        checkout.setShippingAmount(new BigDecimal("4.90"));
        checkout.setTotalAmount(new BigDecimal("70.90"));
        checkout.setCustomerName("Ada Lovelace");
        checkout.setCustomerEmail("ada@example.com");
        checkout.setCustomerPhone("+33600000000");
        checkout.setShippingAddress(new CheckoutShippingAddress(
                "Ada Lovelace", "1 rue des Arts", null, "54000", "Nancy", "FR"));

        cart = new Cart();
        cart.setId(cartId);
        when(checkoutProvider.readIds(any(), any(), any())).thenAnswer(inv -> Stream.of(checkoutId));
        when(checkoutProvider.read(checkoutId)).thenReturn(Optional.of(checkout));
        when(cartProvider.read(cartId)).thenReturn(Optional.of(cart));
    }

    @Test
    void handle_recordsThePaidOrderAndEmptiesTheCart() throws Exception {
        var result = confirm();

        var order = result.order();
        assertEquals(OrderState.PAID, order.getState());
        assertEquals("EOSA-2026-0001", order.getNumber());
        assertEquals("cs_test_1", order.getStripeSessionId().getStripe());
        assertEquals("ada@example.com", order.getCustomerEmail());
        assertEquals("Nancy", order.getShippingAddress().city());
        assertEquals(1, order.getItems().size());
        assertEquals("Collier Lune", order.getItems().getFirst().getName());
        assertEquals(new BigDecimal("66.00"), order.getItemsAmount());
        assertEquals(new BigDecimal("4.90"), order.getShippingAmount());
        assertEquals(new BigDecimal("70.90"), order.getTotalAmount());
        assertEquals(now, order.getPaidAt());
        assertSame(checkout, result.checkout());
        assertTrue(cart.getItems().isEmpty());
        verify(cartProvider).write(cart);
    }

    @Test
    void handle_createsOneOrderEvenWhenThePageIsReloaded() throws Exception {
        var first = confirm().order();
        var second = confirm().order();

        assertEquals(1, orders.all().size());
        assertSame(first, second);
    }

    @Test
    void handle_refusesAnUnpaidSessionWithoutCreatingAnOrder() {
        checkout.setStatus(CheckoutStatus.PENDING);

        assertThrows(RuntimeException.class, this::confirm);

        assertTrue(orders.all().isEmpty());
        verify(cartProvider, never()).write(any());
    }

    @Test
    void handle_refusesAMissingSessionIdentifier() {
        assertThrows(RuntimeException.class, () -> useCase().handle(new ConfirmCheckoutSession.Command(null)));
    }

    @Test
    void handle_sendsTheConfirmationAndTheNotificationOnceAndRecordsThem() throws Exception {
        var order = confirm().order();
        confirm();

        var mails = ArgumentCaptor.forClass(Mail.class);
        verify(mailProvider, times(2)).send(mails.capture());
        assertEquals(List.of("ada@example.com", "contact@eosa.me"), mails.getAllValues().stream().map(Mail::to).toList());
        assertEquals(now, order.getConfirmationSentAt());
        assertEquals(now, order.getNotificationSentAt());
    }

    @Test
    void handle_stillRecordsTheOrderWhenAnEmailFailsAndLeavesItUnsent() throws Exception {
        doThrow(new RuntimeException("SMTP indisponible")).when(mailProvider).send(any());

        var order = confirm().order();

        assertEquals(1, orders.all().size());
        assertNull(order.getConfirmationSentAt());
        assertNull(order.getNotificationSentAt());
        assertTrue(cart.getItems().isEmpty());
    }

    private ConfirmCheckoutSession useCase() {
        return new ConfirmCheckoutSession(checkoutProvider, cartProvider, orders, mailProvider,
                Clock.fixed(now, ZoneOffset.UTC), URI.create("https://admin.eosa.me"));
    }

    private ConfirmCheckoutSession.Result confirm() throws Exception {
        return useCase().handle(new ConfirmCheckoutSession.Command(sessionId));
    }

    private CheckoutItem item(String name, String unitPrice, int quantity) {
        var item = new CheckoutItem();
        item.setName(name);
        item.setUnitPrice(new BigDecimal(unitPrice));
        item.setQuantity(quantity);
        return item;
    }

}
