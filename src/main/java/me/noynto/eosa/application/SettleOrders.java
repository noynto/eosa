package me.noynto.eosa.application;

import me.noynto.eosa.checkout.Checkout;
import me.noynto.eosa.checkout.CheckoutProvider;
import me.noynto.eosa.checkout.CheckoutStatus;
import me.noynto.eosa.mail.MailProvider;
import me.noynto.eosa.order.Order;
import me.noynto.eosa.order.OrderProvider;
import me.noynto.eosa.order.OrderState;
import me.noynto.eosa.service.OrderMails;
import me.noynto.eosa.service.OrdersFromCheckouts;
import me.noynto.eosa.shared.CheckoutId;
import me.noynto.eosa.shared.CheckoutSessionId;
import me.noynto.eosa.shared.OrderId;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Rattrape les paiements Stripe dont la cliente n'est pas revenue sur la page de succès : toute session payée
 * récemment qui n'a pas de commande en reçoit une, comme à la confirmation du paiement, et ses e-mails partent.
 * Renvoie aussi les e-mails de commande qui ne sont pas partis sur le moment. Le panier n'est pas vidé (on ne sait
 * pas à quel navigateur il appartient), et seules les sessions créées par le site (issues d'un panier) comptent.
 */
public record SettleOrders(
        CheckoutProvider checkoutProvider,
        OrderProvider orderProvider,
        MailProvider mailProvider,
        Clock clock,
        URI adminBaseUrl
) {

    /**
     * Fenêtre de recherche : une session de paiement expire au bout de 24 h, et la marge couvre une panne de la tâche.
     */
    public static final Duration LOOKBACK = Duration.ofDays(3);

    /**
     * Laisse le temps à l'envoi immédiat d'avoir lieu, pour ne pas envoyer deux fois le même e-mail.
     */
    static final Duration EMAIL_GRACE_PERIOD = Duration.ofMinutes(5);

    /**
     * Au-delà, une confirmation de commande arriverait trop tard pour servir : on cesse de la renvoyer.
     */
    static final Duration EMAIL_RETRY_WINDOW = Duration.ofDays(7);

    /**
     * Une commande expédiée est souvent plus ancienne que sa création : on regarde les expédiées et livrées des 30
     * derniers jours (date de création) pour retrouver un e-mail d'expédition resté en attente.
     */
    static final Duration SHIPPING_LOOKBACK = Duration.ofDays(30);

    private static final Logger LOGGER = Logger.getLogger(SettleOrders.class.getName());

    public Result handle() {
        Instant now = clock.instant();
        List<CheckoutId> paid = checkoutProvider.readIds(null, CheckoutStatus.COMPLETED, now.minus(LOOKBACK)).toList();

        int settled = 0;
        int emailsSent = 0;
        int failed = 0;
        for (CheckoutId checkoutId : paid) {
            try {
                Order order = settle(checkoutId, now);
                if (order != null) {
                    settled++;
                    var sent = sendMissingEmails(order, now);
                    emailsSent += sent.sent();
                    failed += sent.failed();
                }
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING, "Le paiement " + checkoutId.getStripe() + " n'a pas pu être rattrapé.", e);
                failed++;
            }
        }

        // Les e-mails des commandes plus anciennes qui ne sont pas partis : commande, puis expédition. Une commande
        // expédiée peut être bien plus ancienne que sa création : on regarde aussi les expédiées et livrées récentes.
        Instant retryBefore = now.minus(EMAIL_GRACE_PERIOD);
        Instant retryAfter = now.minus(EMAIL_RETRY_WINDOW);
        Set<OrderId> candidates = new LinkedHashSet<>(orderProvider.readIds(null, null, retryAfter).toList());
        candidates.addAll(orderProvider.readIds(null, Set.of(OrderState.SHIPPED, OrderState.DELIVERED),
                now.minus(SHIPPING_LOOKBACK)).toList());
        for (var orderId : candidates) {
            try {
                Order order = orderProvider.read(orderId).orElseThrow();
                // Une commande annulée n'envoie plus d'e-mail.
                if (order.getState() == OrderState.CANCELLED) {
                    continue;
                }
                boolean orderEmailsPending = (order.getConfirmationSentAt() == null || order.getNotificationSentAt() == null)
                        && order.getCreatedAt().isAfter(retryAfter) && order.getCreatedAt().isBefore(retryBefore);
                boolean shippingEmailPending = order.getShippedAt() != null && order.getShippingSentAt() == null
                        && order.getShippedAt().isAfter(retryAfter) && order.getShippedAt().isBefore(retryBefore)
                        && (order.getState() == OrderState.SHIPPED || order.getState() == OrderState.DELIVERED);
                if (orderEmailsPending) {
                    var sent = sendMissingEmails(order, now);
                    emailsSent += sent.sent();
                    failed += sent.failed();
                }
                if (shippingEmailPending) {
                    var sent = sendMissingShippingEmail(order, now);
                    emailsSent += sent.sent();
                    failed += sent.failed();
                }
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING, "Les e-mails de la commande " + orderId.value() + " n'ont pas pu être renvoyés.", e);
                failed++;
            }
        }
        return new Result(settled, emailsSent, failed);
    }

    private EmailCount sendMissingShippingEmail(Order order, Instant now) {
        try {
            mailProvider.send(OrderMails.shipped(order));
            order.setShippingSentAt(now);
            orderProvider.write(order);
            return new EmailCount(1, 0);
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "L'e-mail d'expédition de la commande " + order.getNumber() + " n'est toujours pas parti.", e);
            return new EmailCount(0, 1);
        }
    }

    private Order settle(CheckoutId checkoutId, Instant now) {
        CheckoutSessionId sessionId = new CheckoutSessionId();
        sessionId.setStripe(checkoutId.getStripe());
        if (orderProvider.readIds(sessionId, null, null).findAny().isPresent()) {
            return null;
        }
        Checkout checkout = checkoutProvider.read(checkoutId)
                .orElseThrow(() -> new RuntimeException("Session Stripe introuvable : " + checkoutId.getStripe()));
        if (checkout.getStatus() != CheckoutStatus.COMPLETED || checkout.getCartId() == null) {
            return null;
        }
        return orderProvider.write(OrdersFromCheckouts.fromPaidCheckout(checkout, now));
    }

    /**
     * Envoie ceux des deux e-mails de la commande qui n'ont pas de date d'envoi, et la note sur la commande.
     */
    private EmailCount sendMissingEmails(Order order, Instant now) {
        int sent = 0;
        int failed = 0;
        if (order.getConfirmationSentAt() == null) {
            try {
                mailProvider.send(OrderMails.confirmation(order));
                order.setConfirmationSentAt(now);
                orderProvider.write(order);
                sent++;
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING, "L'e-mail de confirmation de la commande " + order.getNumber() + " n'est toujours pas parti.", e);
                failed++;
            }
        }
        if (order.getNotificationSentAt() == null) {
            try {
                mailProvider.send(OrderMails.notification(order, adminBaseUrl));
                order.setNotificationSentAt(now);
                orderProvider.write(order);
                sent++;
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING, "L'e-mail de nouvelle commande " + order.getNumber() + " n'est toujours pas parti.", e);
                failed++;
            }
        }
        return new EmailCount(sent, failed);
    }

    private record EmailCount(int sent, int failed) {
    }

    /**
     * @param settled    commandes créées par ce passage
     * @param emailsSent e-mails de commande partis pendant ce passage
     * @param failed     paiements non rattrapés et e-mails non partis (repris au prochain passage)
     */
    public record Result(
            int settled,
            int emailsSent,
            int failed
    ) {
    }

}
