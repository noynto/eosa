package me.noynto.eosa.application;

import me.noynto.eosa.order.Order;
import me.noynto.eosa.order.OrderProvider;
import me.noynto.eosa.order.OrderState;
import me.noynto.eosa.shared.OrderId;

import java.time.Clock;

/**
 * « Livrée » : la commande expédiée passe à livrée. Aucun e-mail. Sans suivi automatique du transporteur, c'est le
 * passage normal en livré ; elle ouvre la durée de conservation de l'adresse et du téléphone.
 */
public record DeliverOrder(
        OrderProvider orderProvider,
        Clock clock
) {

    public Result handle(Command command) {
        if (command.orderId == null || command.orderId.value() == null) {
            throw new RuntimeException("L'identifiant de la commande est nécessaire.");
        }
        Order order = orderProvider.read(command.orderId())
                .orElseThrow(() -> new RuntimeException("La commande " + command.orderId.value() + " n'existe pas."));
        if (order.getState() != OrderState.SHIPPED) {
            throw new RuntimeException("La commande " + order.getNumber() + " n'est pas expédiée : elle ne peut pas être livrée.");
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
