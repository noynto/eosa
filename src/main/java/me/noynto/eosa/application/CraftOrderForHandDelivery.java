package me.noynto.eosa.application;

import me.noynto.eosa.order.Order;
import me.noynto.eosa.order.OrderProvider;
import me.noynto.eosa.order.OrderState;
import me.noynto.eosa.shared.OrderId;

import java.time.Clock;

/**
 * La pièce est terminée et sera remise en main propre : la commande passe à fabriquée sans transporteur, sans
 * étiquette et sans e-mail à la cliente (la remise se convient avec elle). Elle ne sera jamais expédiée : elle passe
 * ensuite à livrée par {@link HandOverOrder}. Les frais de livraison déjà payés ne sont pas remboursés ici : le
 * remboursement se fait dans Stripe, à la main.
 */
public record CraftOrderForHandDelivery(
        OrderProvider orderProvider,
        Clock clock
) {

    public Result handle(Command command) {
        if (command.orderId == null || command.orderId.value() == null) {
            throw new RuntimeException("L'identifiant de la commande est nécessaire.");
        }
        Order order = orderProvider.read(command.orderId())
                .orElseThrow(() -> new RuntimeException("La commande " + command.orderId.value() + " n'existe pas."));
        if (order.getState() != OrderState.PAID || order.getLabel() != null) {
            throw new RuntimeException("La commande " + order.getNumber() + " n'est pas à fabriquer.");
        }
        order.setHandDelivery(true);
        order.setState(OrderState.CRAFTED);
        order.setCraftedAt(clock.instant());
        orderProvider.write(order);
        return new Result(order);
    }

    public record Command(
            OrderId orderId
    ) {
    }

    public record Result(
            Order order
    ) {
    }

}
