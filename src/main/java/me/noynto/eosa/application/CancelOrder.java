package me.noynto.eosa.application;

import me.noynto.eosa.order.Order;
import me.noynto.eosa.order.OrderProvider;
import me.noynto.eosa.order.OrderState;
import me.noynto.eosa.shared.OrderId;

import java.time.Clock;
import java.util.Set;

/**
 * Annule une commande à fabriquer, fabriquée ou expédiée, avec un motif. Aucun e-mail n'est envoyé et rien n'est
 * remboursé : le remboursement se fait dans Stripe, à la main. L'étiquette d'une commande déjà fabriquée reste
 * chez le transporteur (aucune annulation d'étiquette n'est documentée par son API).
 */
public record CancelOrder(
        OrderProvider orderProvider,
        Clock clock
) {

    static final int MAX_REASON_LENGTH = 500;

    private static final Set<OrderState> CANCELLABLE = Set.of(OrderState.PAID, OrderState.CRAFTED, OrderState.SHIPPED);

    public Result handle(Command command) {
        if (command.orderId == null || command.orderId.value() == null) {
            throw new RuntimeException("L'identifiant de la commande est nécessaire.");
        }
        String reason = command.reason == null ? "" : command.reason.strip();
        if (reason.isEmpty()) {
            throw new RuntimeException("Le motif de l'annulation est nécessaire.");
        }
        if (reason.length() > MAX_REASON_LENGTH) {
            throw new RuntimeException("Le motif de l'annulation est limité à " + MAX_REASON_LENGTH + " caractères.");
        }
        Order order = orderProvider.read(command.orderId())
                .orElseThrow(() -> new RuntimeException("La commande " + command.orderId.value() + " n'existe pas."));
        if (!CANCELLABLE.contains(order.getState())) {
            throw new RuntimeException("La commande " + order.getNumber() + " ne peut plus être annulée.");
        }
        order.setState(OrderState.CANCELLED);
        order.setCancelledAt(clock.instant());
        order.setCancelReason(reason);
        orderProvider.write(order);
        return new Result(order);
    }

    public record Command(
            OrderId orderId,
            String reason
    ) {
    }

    public record Result(
            Order order
    ) {
    }

}
