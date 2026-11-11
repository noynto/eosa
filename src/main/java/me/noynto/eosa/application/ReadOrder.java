package me.noynto.eosa.application;

import me.noynto.eosa.order.Order;
import me.noynto.eosa.order.OrderProvider;
import me.noynto.eosa.order.OrderState;
import me.noynto.eosa.service.BusinessTime;
import me.noynto.eosa.shared.OrderId;

import java.time.Clock;
import java.time.Instant;

/**
 * Une commande pour l'écran de détail de l'administration, avec l'échéance de sa fabrication.
 */
public record ReadOrder(
        OrderProvider orderProvider,
        Clock clock
) {

    public Result handle(Command command) {
        if (command.orderId == null || command.orderId.value() == null) {
            throw new RuntimeException("L'identifiant de la commande est nécessaire.");
        }
        Order order = orderProvider.read(command.orderId())
                .orElseThrow(() -> new RuntimeException("La commande " + command.orderId.value() + " n'existe pas."));
        Instant craftingDeadline = BusinessTime.deadline(order.getPaidAt(), Order.CRAFTING_HOURS);
        boolean overdue = order.getState() == OrderState.PAID && clock.instant().isAfter(craftingDeadline);
        return new Result(order, craftingDeadline, overdue);
    }

    public record Command(
            OrderId orderId
    ) {
    }

    /**
     * @param craftingDeadline échéance de fabrication (48 h ouvrées après le paiement)
     * @param overdue          la commande est à fabriquer et son échéance est dépassée
     */
    public record Result(
            Order order,
            Instant craftingDeadline,
            boolean overdue
    ) {
    }

}
