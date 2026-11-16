package me.noynto.eosa.application;

import me.noynto.eosa.giftcard.GiftCard;
import me.noynto.eosa.giftcard.GiftCardMovement;
import me.noynto.eosa.giftcard.GiftCardMovementKind;
import me.noynto.eosa.giftcard.GiftCardProvider;
import me.noynto.eosa.order.Order;
import me.noynto.eosa.order.OrderProvider;
import me.noynto.eosa.shared.CheckoutSessionId;
import me.noynto.eosa.shared.GiftCardId;

import java.util.List;

/**
 * Une carte cadeau avec son journal et les commandes qu'elle a payées en partie, pour l'administration. Les
 * commandes se retrouvent par la session de paiement de chaque utilisation du journal.
 */
public record ReadGiftCard(
        GiftCardProvider giftCardProvider,
        OrderProvider orderProvider
) {

    public Result handle(Command command) {
        if (command.giftCardId == null || command.giftCardId.value() == null) {
            throw new RuntimeException("L'identifiant de la carte est nécessaire.");
        }
        GiftCard giftCard = giftCardProvider.read(command.giftCardId)
                .orElseThrow(() -> new RuntimeException("La carte " + command.giftCardId.value() + " n'existe pas."));
        List<Order> orders = giftCard.getMovements().stream()
                .filter(movement -> movement.getKind() == GiftCardMovementKind.USED && movement.getCheckoutSessionId() != null)
                .map(GiftCardMovement::getCheckoutSessionId)
                .distinct()
                .flatMap(session -> {
                    var sessionId = new CheckoutSessionId();
                    sessionId.setStripe(session);
                    return orderProvider.readIds(sessionId, null, null, null);
                })
                .map(id -> orderProvider.read(id).orElseThrow())
                .toList();
        return new Result(giftCard, orders);
    }

    public record Result(
            GiftCard giftCard,
            List<Order> orders
    ) {
    }

    public record Command(
            GiftCardId giftCardId
    ) {
    }

}
