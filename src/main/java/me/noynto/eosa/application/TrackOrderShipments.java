package me.noynto.eosa.application;

import me.noynto.eosa.mail.MailProvider;
import me.noynto.eosa.order.Order;
import me.noynto.eosa.order.OrderProvider;
import me.noynto.eosa.order.OrderState;
import me.noynto.eosa.service.OrderMails;
import me.noynto.eosa.shared.OrderId;
import me.noynto.eosa.shared.ShippingLabelId;
import me.noynto.eosa.shipping.ShippingLabel;
import me.noynto.eosa.shipping.ShippingLabelProvider;
import me.noynto.eosa.shipping.ShippingLabelStatus;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Suit les colis chez le transporteur : pour chaque commande fabriquée ou expédiée, relit son étiquette. Un colis
 * pris en charge par le transporteur passe la commande à expédiée, avec la date du transporteur et l'e-mail de suivi
 * à la cliente ; un colis livré la passe à livrée. Une commande en erreur n'arrête pas les autres. Idempotente :
 * une commande déjà passée n'est pas retraitée et l'e-mail d'expédition n'est jamais doublé (s'il échoue,
 * {@link SettleOrders} le renvoie). Les boutons « Colis remis » et « Livrée » restent le secours du jour où le
 * transporteur ne répond pas.
 */
public record TrackOrderShipments(
        OrderProvider orderProvider,
        ShippingLabelProvider shippingLabelProvider,
        MailProvider mailProvider,
        Clock clock
) {

    private static final Logger LOGGER = Logger.getLogger(TrackOrderShipments.class.getName());

    public Result handle() {
        Instant now = clock.instant();
        List<OrderId> ids = orderProvider.readIds(null, Set.of(OrderState.CRAFTED, OrderState.SHIPPED), null).toList();

        int shipped = 0;
        int delivered = 0;
        int emailsSent = 0;
        int failed = 0;
        for (OrderId orderId : ids) {
            try {
                Order order = orderProvider.read(orderId).orElseThrow();
                if (order.getLabel() == null) {
                    continue;
                }
                ShippingLabel tracked = shippingLabelProvider.read(new ShippingLabelId(order.getLabel().getTrackingNumber()))
                        .orElseThrow(() -> new RuntimeException("Le transporteur ne connaît pas le colis "
                                + order.getLabel().getTrackingNumber() + " de la commande " + order.getNumber() + "."));
                if (tracked.getStatus() == ShippingLabelStatus.CREATED) {
                    continue;
                }

                boolean justShipped = false;
                if (order.getState() == OrderState.CRAFTED) {
                    order.setState(OrderState.SHIPPED);
                    order.setShippedAt(earlier(tracked.getDepositedAt(), now));
                    justShipped = true;
                    shipped++;
                }
                if (tracked.getStatus() == ShippingLabelStatus.DELIVERED && order.getState() == OrderState.SHIPPED) {
                    order.setState(OrderState.DELIVERED);
                    order.setDeliveredAt(earlier(tracked.getDeliveredAt(), now));
                    delivered++;
                }
                order.getLabel().setStatus(tracked.getStatus());
                orderProvider.write(order);

                if (justShipped) {
                    try {
                        mailProvider.send(OrderMails.shipped(order));
                        order.setShippingSentAt(now);
                        orderProvider.write(order);
                        emailsSent++;
                    } catch (RuntimeException e) {
                        // La commande est bien passée expédiée : SettleOrders renverra l'e-mail.
                        LOGGER.log(Level.WARNING, "L'e-mail d'expédition de la commande " + order.getNumber() + " n'est pas parti, il sera renvoyé.", e);
                        failed++;
                    }
                }
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING, "Le suivi de la commande " + orderId.value() + " a échoué.", e);
                failed++;
            }
        }
        return new Result(shipped, delivered, emailsSent, failed);
    }

    /** La date du transporteur, sans jamais dépasser l'instant présent ; à défaut, maintenant. */
    private static Instant earlier(Instant carrierDate, Instant now) {
        return carrierDate == null || carrierDate.isAfter(now) ? now : carrierDate;
    }

    /**
     * @param shipped    commandes passées à expédiée par ce passage
     * @param delivered  commandes passées à livrée par ce passage
     * @param emailsSent e-mails d'expédition partis pendant ce passage
     * @param failed     commandes dont le suivi a échoué et e-mails non partis (repris au prochain passage)
     */
    public record Result(
            int shipped,
            int delivered,
            int emailsSent,
            int failed
    ) {
    }

}
