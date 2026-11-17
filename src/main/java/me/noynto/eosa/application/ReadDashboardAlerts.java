package me.noynto.eosa.application;

import me.noynto.eosa.giftcard.GiftCard;
import me.noynto.eosa.giftcard.GiftCardMovement;
import me.noynto.eosa.giftcard.GiftCardMovementKind;
import me.noynto.eosa.giftcard.GiftCardProvider;
import me.noynto.eosa.giftcard.GiftCardState;
import me.noynto.eosa.order.Order;
import me.noynto.eosa.order.OrderProvider;
import me.noynto.eosa.order.OrderState;
import me.noynto.eosa.shared.GiftCardId;
import me.noynto.eosa.shared.OrderId;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Ce qui demande un coup d'œil : les e-mails de commande et de carte cadeau qui ne sont pas partis. Les tâches de
 * rattrapage les renvoient ; l'alerte dit seulement qu'ils ne sont pas encore partis. Les mêmes fenêtres que ces
 * tâches : un e-mail récent (moins de 5 minutes) est encore en train de partir, un e-mail de plus de 7 jours n'est
 * plus renvoyé.
 */
public record ReadDashboardAlerts(
        OrderProvider orderProvider,
        GiftCardProvider giftCardProvider,
        Clock clock
) {

    static final Duration GRACE_PERIOD = Duration.ofMinutes(5);
    static final Duration RETRY_WINDOW = Duration.ofDays(7);

    public Result handle() {
        Instant now = clock.instant();
        Instant youngest = now.minus(GRACE_PERIOD);
        Instant oldest = now.minus(RETRY_WINDOW);

        List<Pending> orders = orderProvider.readIds(null, null, oldest, null)
                .map(id -> orderProvider.read(id).orElseThrow())
                .filter(order -> order.getState() != OrderState.CANCELLED)
                .filter(order -> order.getCreatedAt().isBefore(youngest))
                .filter(order -> order.getConfirmationSentAt() == null || order.getNotificationSentAt() == null)
                .map(order -> new Pending(order.getId().value(), order.getNumber()))
                .toList();

        List<GiftCardId> giftCards = giftCardProvider.readIds(null, null)
                .map(id -> giftCardProvider.read(id).orElseThrow())
                .filter(giftCard -> giftCard.getState() == GiftCardState.ACTIVE && giftCard.getEmailSentAt() == null
                        && giftCard.getBuyerEmail() != null)
                .filter(giftCard -> {
                    Instant issuedAt = issuedAt(giftCard);
                    return issuedAt.isBefore(youngest) && issuedAt.isAfter(oldest);
                })
                .map(GiftCard::getId)
                .toList();

        return new Result(orders, giftCards);
    }

    private static Instant issuedAt(GiftCard giftCard) {
        return giftCard.getMovements().stream()
                .filter(movement -> movement.getKind() == GiftCardMovementKind.ISSUED)
                .map(GiftCardMovement::getCreatedAt)
                .findFirst()
                .orElse(giftCard.getCreatedAt());
    }

    /** Une commande dont un e-mail n'est pas parti : son identifiant et son numéro, pour y aller. */
    public record Pending(String orderId, String number) {
    }

    /**
     * @param orders    commandes dont l'e-mail de confirmation ou de notification n'est pas parti
     * @param giftCards cartes actives dont l'e-mail n'est pas parti
     */
    public record Result(
            List<Pending> orders,
            List<GiftCardId> giftCards
    ) {

        public boolean isEmpty() {
            return orders.isEmpty() && giftCards.isEmpty();
        }
    }

}
