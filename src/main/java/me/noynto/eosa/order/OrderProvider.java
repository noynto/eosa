package me.noynto.eosa.order;

import me.noynto.eosa.shared.CheckoutSessionId;
import me.noynto.eosa.shared.OrderId;
import me.noynto.eosa.shared.PromoCodeId;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public interface OrderProvider {

    /**
     * Identifiants des commandes, de la plus récente à la plus ancienne. Chaque filtre est ignoré
     * s'il est null. Avec {@code promoCodeId}, seules les commandes passées avec ce code de promotion, quel que
     * soit leur état : une commande annulée garde son usage.
     */
    Stream<OrderId> readIds(CheckoutSessionId stripeSessionId, Set<OrderState> states, Instant createdSince,
                            PromoCodeId promoCodeId);

    Optional<Order> read(OrderId orderId);

    /**
     * Écrit la commande. À la première écriture (sans identifiant), attribue l'identifiant et le numéro.
     * Si une commande existe déjà pour la même session Stripe, rien n'est créé et c'est elle qui est rendue.
     */
    Order write(Order order);

    void delete(OrderId orderId);

}
