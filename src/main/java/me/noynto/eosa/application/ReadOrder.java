package me.noynto.eosa.application;

import me.noynto.eosa.order.Order;
import me.noynto.eosa.order.OrderProvider;
import me.noynto.eosa.order.OrderState;
import me.noynto.eosa.service.BusinessTime;
import me.noynto.eosa.shared.OrderId;
import me.noynto.eosa.shipping.ParcelDefaults;

import java.time.Clock;
import java.time.Instant;

/**
 * Une commande pour l'écran de détail de l'administration, avec l'échéance de sa fabrication ou de son expédition.
 *
 * @param parcelDefaults poids et dimensions du colis proposés à la fabrication
 */
public record ReadOrder(
        OrderProvider orderProvider,
        Clock clock,
        ParcelDefaults parcelDefaults
) {

    public Result handle(Command command) {
        if (command.orderId == null || command.orderId.value() == null) {
            throw new RuntimeException("L'identifiant de la commande est nécessaire.");
        }
        Order order = orderProvider.read(command.orderId())
                .orElseThrow(() -> new RuntimeException("La commande " + command.orderId.value() + " n'existe pas."));
        Instant now = clock.instant();
        Instant craftingDeadline = BusinessTime.deadline(order.getPaidAt(), Order.CRAFTING_HOURS);
        Instant shippingDeadline = order.getCraftedAt() != null
                ? BusinessTime.deadline(order.getCraftedAt(), Order.SHIPPING_HOURS) : null;
        boolean overdue = order.getState() == OrderState.PAID && now.isAfter(craftingDeadline)
                // Une remise en main propre se convient avec la cliente : pas d'échéance d'expédition.
                || order.getState() == OrderState.CRAFTED && !order.isHandDelivery() && shippingDeadline != null && now.isAfter(shippingDeadline);
        return new Result(order, craftingDeadline, shippingDeadline, overdue, parcelDefaults);
    }

    public record Command(
            OrderId orderId
    ) {
    }

    /**
     * @param craftingDeadline         échéance de fabrication (48 h ouvrées après le paiement)
     * @param shippingDeadline         échéance d'expédition (24 h ouvrées après la fabrication), null tant que la
     *                                 commande n'est pas fabriquée
     * @param overdue                  la commande est à fabriquer ou à expédier et son échéance est dépassée
     * @param parcelDefaults           poids et dimensions du colis proposés à la fabrication
     */
    public record Result(
            Order order,
            Instant craftingDeadline,
            Instant shippingDeadline,
            boolean overdue,
            ParcelDefaults parcelDefaults
    ) {
    }

}
