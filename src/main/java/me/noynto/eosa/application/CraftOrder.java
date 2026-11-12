package me.noynto.eosa.application;

import me.noynto.eosa.order.Order;
import me.noynto.eosa.order.OrderProvider;
import me.noynto.eosa.order.OrderState;
import me.noynto.eosa.shared.OrderId;
import me.noynto.eosa.shipping.ShippingLabel;
import me.noynto.eosa.shipping.ShippingLabelProvider;
import me.noynto.eosa.shipping.ShippingRecipient;

import java.time.Clock;

/**
 * La pièce est terminée : crée l'étiquette d'expédition chez le transporteur et passe la commande à fabriquée.
 * Si le transporteur refuse ou ne répond pas, rien ne change et la commande reste à fabriquer : jamais de commande
 * fabriquée sans étiquette. Si l'écriture de la commande échoue après la création de l'étiquette, une étiquette
 * orpheline reste chez le transporteur (la création n'est pas idempotente chez lui).
 */
public record CraftOrder(
        OrderProvider orderProvider,
        ShippingLabelProvider shippingLabelProvider,
        Clock clock
) {

    public Result handle(Command command) {
        if (command.orderId == null || command.orderId.value() == null) {
            throw new RuntimeException("L'identifiant de la commande est nécessaire.");
        }
        if (command.weightGrams <= 0) {
            throw new RuntimeException("Le poids du colis doit être un nombre de grammes positif.");
        }
        Order order = orderProvider.read(command.orderId())
                .orElseThrow(() -> new RuntimeException("La commande " + command.orderId.value() + " n'existe pas."));
        if (order.getState() != OrderState.PAID || order.getLabel() != null) {
            throw new RuntimeException("La commande " + order.getNumber() + " n'est pas à fabriquer.");
        }
        var address = order.getShippingAddress();
        if (address == null) {
            throw new RuntimeException("La commande " + order.getNumber() + " n'a plus d'adresse de livraison.");
        }

        ShippingLabel label = new ShippingLabel();
        label.setOrderNumber(order.getNumber());
        label.setWeightGrams(command.weightGrams);
        label.setRecipient(new ShippingRecipient(
                address.name() != null ? address.name() : order.getCustomerName(),
                order.getCustomerEmail(), order.getCustomerPhone(),
                address.line1(), address.line2(), address.postalCode(), address.city(), address.country()));
        label = shippingLabelProvider.write(label);

        label.setCreatedAt(clock.instant());
        // L'adresse ne reste pas avec l'étiquette : elle est dans la commande.
        label.setRecipient(null);
        order.setLabel(label);
        order.setCraftedAt(label.getCreatedAt());
        order.setState(OrderState.CRAFTED);
        orderProvider.write(order);
        return new Result(order, label.getTrackingNumber());
    }

    /**
     * @param weightGrams poids du colis confirmé par l'administratrice
     */
    public record Command(
            OrderId orderId,
            int weightGrams
    ) {
    }

    public record Result(
            Order order,
            String trackingNumber
    ) {
    }

}
