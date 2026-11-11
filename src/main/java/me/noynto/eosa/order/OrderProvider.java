package me.noynto.eosa.order;

import me.noynto.eosa.shared.CheckoutSessionId;
import me.noynto.eosa.shared.OrderId;

import java.util.Optional;
import java.util.stream.Stream;

public interface OrderProvider {

    /**
     * Identifiants des commandes, de la plus récente à la plus ancienne. Le filtre est ignoré
     * s'il est null.
     */
    Stream<OrderId> readIds(CheckoutSessionId stripeSessionId);

    Optional<Order> read(OrderId orderId);

    /**
     * Écrit la commande. À la première écriture (sans identifiant), attribue l'identifiant et le numéro.
     * Si une commande existe déjà pour la même session Stripe, rien n'est créé et c'est elle qui est rendue.
     */
    Order write(Order order);

    void delete(OrderId orderId);

}
