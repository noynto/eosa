package me.noynto.eosa.application;

import me.noynto.eosa.order.Order;
import me.noynto.eosa.order.OrderProvider;
import me.noynto.eosa.order.OrderState;
import me.noynto.eosa.shared.OrderId;

import java.time.Clock;

/**
 * « Remise effectuée » : la commande fabriquée pour une remise en main propre passe à livrée. Aucun e-mail. La
 * date ouvre la durée de conservation de l'adresse et du téléphone, comme une livraison par transporteur.
 */
public record HandOverOrder(
        OrderProvider orderProvider,
        Clock clock
) {

    public Result handle(Command command) {
        if (command.orderId == null || command.orderId.value() == null) {
            throw new RuntimeException("L'identifiant de la commande est nécessaire.");
        }
        Order order = orderProvider.read(command.orderId())
                .orElseThrow(() -> new RuntimeException("La commande " + command.orderId.value() + " n'existe pas."));
        if (order.getState() != OrderState.CRAFTED || !order.isHandDelivery()) {
            throw new RuntimeException("La commande " + order.getNumber() + " n'est pas fabriquée pour une remise en main propre.");
        }
        order.setState(OrderState.DELIVERED);
        order.setDeliveredAt(clock.instant());
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
