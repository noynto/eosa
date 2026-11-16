package me.noynto.eosa.application;

import me.noynto.eosa.mail.MailProvider;
import me.noynto.eosa.order.Order;
import me.noynto.eosa.order.OrderProvider;
import me.noynto.eosa.order.OrderState;
import me.noynto.eosa.service.OrderMails;
import me.noynto.eosa.shared.OrderId;

import java.time.Clock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * « Colis remis » : la commande fabriquée passe à expédiée et la cliente reçoit l'e-mail avec le lien de suivi.
 * La transition ne dépend jamais de l'e-mail : s'il échoue, la commande reste expédiée et {@link SettleOrders}
 * le renvoie. Sans suivi automatique du transporteur, c'est le passage normal en expédié.
 */
public record ShipOrder(
        OrderProvider orderProvider,
        MailProvider mailProvider,
        Clock clock
) {

    private static final Logger LOGGER = Logger.getLogger(ShipOrder.class.getName());

    public Result handle(Command command) {
        if (command.orderId == null || command.orderId.value() == null) {
            throw new RuntimeException("L'identifiant de la commande est nécessaire.");
        }
        Order order = orderProvider.read(command.orderId())
                .orElseThrow(() -> new RuntimeException("La commande " + command.orderId.value() + " n'existe pas."));
        if (order.isHandDelivery()) {
            throw new RuntimeException("La commande " + order.getNumber() + " est remise en main propre : elle n'est pas expédiée.");
        }
        if (order.getState() != OrderState.CRAFTED || order.getLabel() == null) {
            throw new RuntimeException("La commande " + order.getNumber() + " n'est pas fabriquée : elle ne peut pas être expédiée.");
        }

        order.setState(OrderState.SHIPPED);
        order.setShippedAt(clock.instant());
        orderProvider.write(order);

        boolean emailSent = false;
        try {
            mailProvider.send(OrderMails.shipped(order));
            order.setShippingSentAt(clock.instant());
            orderProvider.write(order);
            emailSent = true;
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "L'e-mail d'expédition de la commande " + order.getNumber() + " n'est pas parti, il sera renvoyé.", e);
        }
        return new Result(order, emailSent);
    }

    public record Command(
            OrderId orderId
    ) {
    }

    /**
     * @param emailSent l'e-mail d'expédition est parti ; sinon la tâche de rattrapage le renverra
     */
    public record Result(
            Order order,
            boolean emailSent
    ) {
    }

}
