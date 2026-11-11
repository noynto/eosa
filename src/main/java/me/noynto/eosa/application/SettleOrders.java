package me.noynto.eosa.application;

import me.noynto.eosa.checkout.Checkout;
import me.noynto.eosa.checkout.CheckoutProvider;
import me.noynto.eosa.checkout.CheckoutStatus;
import me.noynto.eosa.order.OrderProvider;
import me.noynto.eosa.service.OrdersFromCheckouts;
import me.noynto.eosa.shared.CheckoutId;
import me.noynto.eosa.shared.CheckoutSessionId;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Rattrape les paiements Stripe dont la cliente n'est pas revenue sur la page de succès : toute session payée
 * récemment qui n'a pas de commande en reçoit une, comme à la confirmation du paiement. Le panier n'est pas vidé
 * (on ne sait pas à quel navigateur il appartient), et seules les sessions créées par le site (issues d'un panier)
 * comptent.
 */
public record SettleOrders(
        CheckoutProvider checkoutProvider,
        OrderProvider orderProvider,
        Clock clock
) {

    /**
     * Fenêtre de recherche : une session de paiement expire au bout de 24 h, et la marge couvre une panne de la tâche.
     */
    public static final Duration LOOKBACK = Duration.ofDays(3);

    public Result handle() {
        Instant now = clock.instant();
        List<CheckoutId> paid = checkoutProvider.readIds(null, CheckoutStatus.COMPLETED, now.minus(LOOKBACK)).toList();

        int settled = 0;
        int failed = 0;
        for (CheckoutId checkoutId : paid) {
            try {
                if (settle(checkoutId, now)) {
                    settled++;
                }
            } catch (RuntimeException e) {
                failed++;
            }
        }
        return new Result(settled, failed);
    }

    private boolean settle(CheckoutId checkoutId, Instant now) {
        CheckoutSessionId sessionId = new CheckoutSessionId();
        sessionId.setStripe(checkoutId.getStripe());
        if (orderProvider.readIds(sessionId, null).findAny().isPresent()) {
            return false;
        }
        Checkout checkout = checkoutProvider.read(checkoutId)
                .orElseThrow(() -> new RuntimeException("Session Stripe introuvable : " + checkoutId.getStripe()));
        if (checkout.getStatus() != CheckoutStatus.COMPLETED || checkout.getCartId() == null) {
            return false;
        }
        orderProvider.write(OrdersFromCheckouts.fromPaidCheckout(checkout, now));
        return true;
    }

    /**
     * @param settled commandes créées par ce passage
     * @param failed  sessions payées qui n'ont pas pu être rattrapées (reprises au prochain passage)
     */
    public record Result(
            int settled,
            int failed
    ) {
    }

}
