package me.noynto.eosa.application;

import me.noynto.eosa.order.Order;
import me.noynto.eosa.order.OrderProvider;
import me.noynto.eosa.shared.CheckoutSessionId;
import me.noynto.eosa.shared.OrderId;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Commandes en mémoire pour les tests, avec la même règle que la persistance JDBC : une commande au plus
 * par session Stripe, numéro attribué à la première écriture.
 */
class InMemoryOrders implements OrderProvider {

    private final List<Order> orders = new ArrayList<>();

    @Override
    public Stream<OrderId> readIds(CheckoutSessionId stripeSessionId) {
        return orders.stream()
                .filter(order -> stripeSessionId == null
                        || stripeSessionId.getStripe().equals(order.getStripeSessionId().getStripe()))
                .sorted(Comparator.comparing(Order::getPaidAt).reversed())
                .map(Order::getId)
                .toList()
                .stream();
    }

    @Override
    public Optional<Order> read(OrderId orderId) {
        return orders.stream().filter(order -> order.getId().equals(orderId)).findFirst();
    }

    @Override
    public Order write(Order order) {
        if (order.getId() == null) {
            var existing = orders.stream()
                    .filter(other -> other.getStripeSessionId().getStripe().equals(order.getStripeSessionId().getStripe()))
                    .findFirst();
            if (existing.isPresent()) {
                return existing.get();
            }
            order.setId(new OrderId(UUID.randomUUID().toString()));
            order.setNumber("EOSA-2026-%04d".formatted(orders.size() + 1));
            order.setCreatedAt(order.getPaidAt());
        }
        orders.removeIf(other -> other.getId().equals(order.getId()));
        orders.add(order);
        return order;
    }

    @Override
    public void delete(OrderId orderId) {
        orders.removeIf(order -> order.getId().equals(orderId));
    }

    List<Order> all() {
        return orders;
    }

}
