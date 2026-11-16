package me.noynto.eosa.application;

import me.noynto.eosa.order.Order;
import me.noynto.eosa.order.OrderProvider;
import me.noynto.eosa.order.OrderState;
import me.noynto.eosa.shared.OrderId;

/**
 * Adresse du PDF de l'étiquette d'une commande fabriquée ou expédiée, à imprimer ou réimprimer. Ne modifie rien.
 */
public record ReadOrderLabel(
        OrderProvider orderProvider
) {

    public Result handle(Command command) {
        if (command.orderId == null || command.orderId.value() == null) {
            throw new RuntimeException("L'identifiant de la commande est nécessaire.");
        }
        Order order = orderProvider.read(command.orderId())
                .orElseThrow(() -> new RuntimeException("La commande " + command.orderId.value() + " n'existe pas."));
        boolean printable = order.getState() == OrderState.CRAFTED || order.getState() == OrderState.SHIPPED;
        if (!printable || order.getLabel() == null) {
            throw new RuntimeException("La commande " + order.getNumber() + " n'a pas d'étiquette à imprimer.");
        }
        return new Result(order.getLabel().getReference());
    }

    public record Command(
            OrderId orderId
    ) {
    }

    /**
     * @param url adresse du PDF chez le transporteur
     */
    public record Result(
            String url
    ) {
    }

}
