package me.noynto.eosa.service;

import me.noynto.eosa.checkout.Checkout;
import me.noynto.eosa.checkout.CheckoutItem;
import me.noynto.eosa.order.Order;
import me.noynto.eosa.order.OrderItem;
import me.noynto.eosa.order.OrderShippingAddress;
import me.noynto.eosa.order.OrderState;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Construit une commande à partir d'un paiement Stripe confirmé. La règle croise deux domaines
 * (paiement et commande) : c'est un service statique, sans provider.
 */
public final class OrdersFromCheckouts {

    private OrdersFromCheckouts() {
    }

    /**
     * Commande {@code PAID} qui copie les lignes, l'adresse et les montants du paiement. Le total est celui
     * payé chez Stripe quand il est connu ; sinon il est recalculé à partir des lignes et de la livraison.
     * L'identifiant et le numéro sont attribués à l'écriture.
     */
    public static Order fromPaidCheckout(Checkout checkout, Instant paidAt) {
        if (checkout.getSession() == null || checkout.getSession().getId() == null) {
            throw new RuntimeException("Une commande doit être issue d'une session de paiement.");
        }
        List<CheckoutItem> checkoutItems = checkout.getItems() != null ? checkout.getItems() : List.of();
        List<OrderItem> items = checkoutItems.stream().map(OrdersFromCheckouts::toOrderItem).toList();

        BigDecimal itemsAmount = items.stream()
                .map(OrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal shippingAmount = checkout.getShippingAmount() != null ? checkout.getShippingAmount() : BigDecimal.ZERO;
        BigDecimal totalAmount = checkout.getTotalAmount() != null
                ? checkout.getTotalAmount()
                : itemsAmount.add(shippingAmount);

        Order order = new Order();
        order.setState(OrderState.PAID);
        order.setStripeSessionId(checkout.getSession().getId());
        order.setCustomerName(checkout.getCustomerName());
        order.setCustomerEmail(checkout.getCustomerEmail());
        order.setCustomerPhone(checkout.getCustomerPhone());
        if (checkout.getShippingAddress() != null) {
            var address = checkout.getShippingAddress();
            order.setShippingAddress(new OrderShippingAddress(
                    address.name(), address.line1(), address.line2(),
                    address.postalCode(), address.city(), address.country()));
        }
        order.setItems(items);
        order.setItemsAmount(itemsAmount);
        order.setShippingAmount(shippingAmount);
        order.setTotalAmount(totalAmount);
        order.setPaidAt(paidAt);
        return order;
    }

    private static OrderItem toOrderItem(CheckoutItem checkoutItem) {
        OrderItem item = new OrderItem();
        item.setName(checkoutItem.getName());
        item.setUnitPrice(checkoutItem.getUnitPrice());
        item.setQuantity(checkoutItem.getQuantity());
        item.setJewelId(checkoutItem.getJewelId());
        item.setImageId(checkoutItem.getImageId());
        return item;
    }

}
