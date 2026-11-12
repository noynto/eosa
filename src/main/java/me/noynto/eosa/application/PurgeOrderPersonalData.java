package me.noynto.eosa.application;

import me.noynto.eosa.order.Order;
import me.noynto.eosa.order.OrderProvider;
import me.noynto.eosa.order.OrderState;
import me.noynto.eosa.service.BusinessTime;
import me.noynto.eosa.shared.OrderId;

import java.time.Clock;
import java.time.Instant;
import java.time.Period;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Efface l'adresse de livraison et le téléphone d'une commande 1 an après sa livraison (ou son annulation), pour
 * couvrir retours et garantie, comme l'annonce la politique de confidentialité. Les montants, les lignes, le nom et
 * l'e-mail de la cliente restent : ce sont des pièces comptables, conservées 10 ans. L'effacement est tracé sur la
 * commande ({@code personalDataPurgedAt}) et ne se fait qu'une fois. Une commande qui n'est ni livrée ni annulée
 * n'est jamais effacée, quel que soit son âge : la cliente attend peut-être encore son colis.
 */
public record PurgeOrderPersonalData(
        OrderProvider orderProvider,
        Clock clock
) {

    public static final Period RETENTION = Period.ofYears(1);

    private static final Logger LOGGER = Logger.getLogger(PurgeOrderPersonalData.class.getName());

    public Result handle() {
        Instant now = clock.instant();
        List<OrderId> ids = orderProvider
                .readIds(null, Set.of(OrderState.DELIVERED, OrderState.CANCELLED), null).toList();

        int purged = 0;
        int failed = 0;
        for (var orderId : ids) {
            try {
                Order order = orderProvider.read(orderId).orElseThrow();
                if (order.getPersonalDataPurgedAt() != null) {
                    continue;
                }
                Instant deleteAfter = referenceDate(order).atZone(BusinessTime.ZONE).plus(RETENTION).toInstant();
                if (now.isBefore(deleteAfter)) {
                    continue;
                }
                order.setShippingAddress(null);
                order.setCustomerPhone(null);
                order.setPersonalDataPurgedAt(now);
                orderProvider.write(order);
                purged++;
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING, "Les données personnelles de la commande " + orderId.value() + " n'ont pas pu être effacées.", e);
                failed++;
            }
        }
        return new Result(purged, failed);
    }

    /**
     * Date qui ouvre la durée de conservation : la livraison, ou l'annulation. À défaut (commande antérieure au suivi
     * de ces dates), la dernière étape connue, pour ne jamais effacer trop tôt.
     */
    private static Instant referenceDate(Order order) {
        Instant date = order.getState() == OrderState.CANCELLED ? order.getCancelledAt() : order.getDeliveredAt();
        if (date == null) {
            date = order.getShippedAt();
        }
        if (date == null) {
            date = order.getCraftedAt();
        }
        return date != null ? date : order.getPaidAt();
    }

    /**
     * @param purged commandes dont l'adresse et le téléphone ont été effacés par ce passage
     * @param failed commandes qui n'ont pas pu être effacées (reprises au prochain passage)
     */
    public record Result(
            int purged,
            int failed
    ) {
    }

}
