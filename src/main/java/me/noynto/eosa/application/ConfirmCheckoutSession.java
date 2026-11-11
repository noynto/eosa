package me.noynto.eosa.application;

import me.noynto.eosa.cart.CartProvider;
import me.noynto.eosa.checkout.Checkout;
import me.noynto.eosa.checkout.CheckoutProvider;
import me.noynto.eosa.checkout.CheckoutStatus;
import me.noynto.eosa.mail.MailProvider;
import me.noynto.eosa.order.Order;
import me.noynto.eosa.order.OrderProvider;
import me.noynto.eosa.service.OrderMails;
import me.noynto.eosa.service.OrdersFromCheckouts;
import me.noynto.eosa.shared.CheckoutSessionId;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public record ConfirmCheckoutSession(
        CheckoutProvider checkoutProvider,
        CartProvider cartProvider,
        OrderProvider orderProvider,
        MailProvider mailProvider,
        Clock clock,
        URI adminBaseUrl
) {

    private static final Logger LOGGER = Logger.getLogger(ConfirmCheckoutSession.class.getName());

    public Result handle(Command command) throws Exception {
        if (command.checkoutSessionId == null || command.checkoutSessionId.getStripe().isBlank()) {
            throw new RuntimeException("L'identifiant de session d'achat est nécessaire.");
        }

        var checkoutSessionId = command.checkoutSessionId;

        // 1. Vérifie le statut du paiement chez Stripe
        Checkout checkout = this.checkoutProvider.readIds(checkoutSessionId, null, null)
                .findFirst()
                .flatMap(this.checkoutProvider::read)
                .orElseThrow(() -> new RuntimeException("Session Stripe introuvable."));

        if (Set.of(CheckoutStatus.PENDING, CheckoutStatus.CANCELLED, CheckoutStatus.EXPIRED).contains(checkout.getStatus())) {
            throw new RuntimeException("Le paiement n'a pas été confirmé.");
        }

        if (checkout.getCartId() == null) {
            throw new RuntimeException("Une commande doit forcément être issu d'un panier.");
        }

        // 2. Enregistre la commande, une seule fois par session de paiement : recharger la page ne crée rien de plus
        //    et n'envoie aucun e-mail de plus.
        Instant now = clock.instant();
        Order order = orderProvider.readIds(checkoutSessionId, null, null)
                .findFirst()
                .flatMap(orderProvider::read)
                .orElse(null);
        if (order == null) {
            order = orderProvider.write(OrdersFromCheckouts.fromPaidCheckout(checkout, now));
            sendEmails(order, now);
        }

        // 3. Vide le panier payé
        cartProvider.read(checkout.getCartId())
                .ifPresent(cart -> {
                    cart.setItems(List.of());
                    cartProvider.write(cart);
                });

        return new Result(checkout, order);
    }

    /**
     * Envoie la confirmation à la cliente et la notification à EOSA. Un e-mail qui ne part pas ne bloque jamais la
     * cliente : il reste sans date d'envoi et la tâche de rattrapage le renvoie.
     */
    private void sendEmails(Order order, Instant now) {
        try {
            mailProvider.send(OrderMails.confirmation(order));
            order.setConfirmationSentAt(now);
            orderProvider.write(order);
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "L'e-mail de confirmation de la commande " + order.getNumber() + " n'est pas parti.", e);
        }
        try {
            mailProvider.send(OrderMails.notification(order, adminBaseUrl));
            order.setNotificationSentAt(now);
            orderProvider.write(order);
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "L'e-mail de nouvelle commande " + order.getNumber() + " n'est pas parti.", e);
        }
    }

    /**
     * @param order la commande enregistrée pour ce paiement, jamais null
     */
    public record Result(
            Checkout checkout,
            Order order
    ) {
    }

    public record Command(
            CheckoutSessionId checkoutSessionId
    ) {
    }

}
