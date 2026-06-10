package me.noynto.eosa.infrastructure.fetch.stripe.adapter;

import me.noynto.eosa.checkout.*;
import me.noynto.eosa.infrastructure.fetch.stripe.resource.StripeCheckoutSessionResource;
import me.noynto.eosa.shared.CartId;
import me.noynto.eosa.shared.CheckoutId;
import me.noynto.eosa.shared.CheckoutSessionId;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public record StripeFetchedCheckouts(
        StripeCheckoutSessionResource checkoutSessionResource,
        String baseUrl
) implements CheckoutProvider {

    @Override
    public Stream<CheckoutId> readIds(CheckoutSessionId sessionId) {
        try {
            var stripeSession = checkoutSessionResource.retrieveSession(sessionId.getStripe());
            return Stream.of(toCheckout(stripeSession).getId());
        } catch (Exception e) {
            return Stream.empty();
        }
    }

    @Override
    public Optional<Checkout> read(CheckoutId id) {
        try {
            var stripeSession = checkoutSessionResource.retrieveSession(id.getStripe());
            var checkout = toCheckout(stripeSession);
            var lineItems = checkoutSessionResource.retrieveLineItems(id.getStripe());
            if (lineItems != null && lineItems.data() != null) {
                checkout.setItems(lineItems.data().stream().map(item -> {
                    CheckoutItem checkoutItem = new CheckoutItem();
                    checkoutItem.setName(item.description());
                    checkoutItem.setQuantity((int) item.quantity());
                    if (item.quantity() > 0) {
                        checkoutItem.setUnitPrice(
                            BigDecimal.valueOf(item.amountTotal())
                                .divide(BigDecimal.valueOf(100))
                                .divide(BigDecimal.valueOf(item.quantity()))
                        );
                    }
                    return checkoutItem;
                }).toList());
            }
            return Optional.of(checkout);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Checkout toCheckout(StripeCheckoutSessionResource.CheckoutSession stripeSession) {
        Checkout checkout = new Checkout();

        CheckoutId id = new CheckoutId();
        id.setStripe(stripeSession.id());
        checkout.setId(id);

        if (stripeSession.clientReferenceId() != null) {
            checkout.setCartId(new CartId(stripeSession.clientReferenceId()));
        }

        checkout.setStatus("paid".equals(stripeSession.paymentStatus())
                ? CheckoutStatus.COMPLETED
                : CheckoutStatus.PENDING);

        CheckoutSession checkoutSession = new CheckoutSession();
        CheckoutSessionId checkoutSessionId = new CheckoutSessionId();
        checkoutSessionId.setStripe(stripeSession.id());
        checkoutSession.setId(checkoutSessionId);
        if (stripeSession.url() != null) {
            checkoutSession.setUri(URI.create(stripeSession.url()));
        }
        checkoutSession.setStatus(CheckoutSessionStatus.OPENED);
        checkout.setSession(checkoutSession);

        return checkout;
    }

    @Override
    public Checkout write(Checkout checkout) {
        try {
            List<StripeCheckoutSessionResource.LineItem> lineItems = checkout.getItems()
                    .stream()
                    .map(checkoutItem -> new StripeCheckoutSessionResource.LineItem(
                            checkoutItem.getName(),
                            checkoutItem.getUnitPrice(),
                            checkoutItem.getQuantity(),
                            checkoutItem.getImageId() != null
                                    ? baseUrl + "/images/" + checkoutItem.getImageId().value()
                                    : null
                    ))
                    .toList();

            String clientReferenceId = checkout.getCartId() != null ? checkout.getCartId().value() : null;

            StripeCheckoutSessionResource.CheckoutSession stripeCheckoutSession = checkoutSessionResource.post(lineItems, clientReferenceId);

            CheckoutSession checkoutSession = new CheckoutSession();
            checkoutSession.setUri(URI.create(stripeCheckoutSession.url()));
            checkoutSession.setStatus(CheckoutSessionStatus.OPENED);
            CheckoutSessionId checkoutSessionId = new CheckoutSessionId();
            checkoutSessionId.setStripe(stripeCheckoutSession.id());
            checkoutSession.setId(checkoutSessionId);
            checkout.setSession(checkoutSession);
            return checkout;
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la création de la session Stripe.", e);
        }
    }

}