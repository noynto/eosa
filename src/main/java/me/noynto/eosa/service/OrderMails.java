package me.noynto.eosa.service;

import me.noynto.eosa.mail.Mail;
import me.noynto.eosa.order.Order;
import me.noynto.eosa.order.OrderItem;

import java.math.BigDecimal;
import java.net.URI;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Les e-mails de commande. Chaque méthode construit le {@link Mail} : l'envoi, et l'enregistrement qu'il est parti,
 * restent au cas d'usage.
 */
public final class OrderMails {

    /**
     * Adresse qui reçoit les nouvelles commandes à fabriquer.
     */
    public static final String NOTIFICATION_RECIPIENT = "contact@eosa.me";

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy 'à' H 'h' mm", Locale.FRENCH);

    private OrderMails() {
    }

    /**
     * Confirmation à la cliente : numéro, lignes, montants, adresse de livraison et délais.
     */
    public static Mail confirmation(Order order) {
        Map<String, Object> model = new HashMap<>();
        model.put("preheader", "Merci pour votre commande " + order.getNumber() + " : chaque pièce est fabriquée à la commande.");
        model.put("hasName", order.getCustomerName() != null && !order.getCustomerName().isBlank());
        model.put("name", order.getCustomerName());
        model.put("number", order.getNumber());
        model.put("items", itemsOf(order));
        model.put("itemsAmount", amount(order.getItemsAmount()));
        model.put("shippingAmount", amount(order.getShippingAmount()));
        model.put("totalAmount", amount(order.getTotalAmount()));
        var address = order.getShippingAddress();
        model.put("hasAddress", address != null);
        if (address != null) {
            model.put("addressName", address.name());
            model.put("addressLine1", address.line1());
            model.put("hasAddressLine2", address.line2() != null && !address.line2().isBlank());
            model.put("addressLine2", address.line2());
            model.put("addressPostalCode", address.postalCode());
            model.put("addressCity", address.city());
            model.put("addressCountry", address.country());
        }
        model.put("contactEmail", NOTIFICATION_RECIPIENT);
        return new Mail(
                order.getCustomerEmail(),
                "Votre commande EOSA " + order.getNumber(),
                "order-confirmation",
                model
        );
    }

    /**
     * Prévient EOSA qu'une commande est à fabriquer, avec le lien vers sa page d'administration.
     */
    public static Mail notification(Order order, URI adminBaseUrl) {
        int pieces = order.getItems().stream().mapToInt(OrderItem::getQuantity).sum();
        Instant craftingDeadline = BusinessTime.deadline(order.getPaidAt(), Order.CRAFTING_HOURS);
        Map<String, Object> model = new HashMap<>();
        model.put("preheader", "Nouvelle commande " + order.getNumber() + " : " + pieces + " pièce(s), " + amount(order.getTotalAmount()) + ".");
        model.put("number", order.getNumber());
        model.put("pieces", pieces);
        model.put("items", itemsOf(order));
        model.put("totalAmount", amount(order.getTotalAmount()));
        model.put("customerName", order.getCustomerName());
        model.put("customerEmail", order.getCustomerEmail());
        model.put("craftingDeadline", DATE_TIME.format(craftingDeadline.atZone(BusinessTime.ZONE)));
        model.put("orderUrl", adminBaseUrl + "/orders/" + order.getId().value());
        return new Mail(
                NOTIFICATION_RECIPIENT,
                "Nouvelle commande " + order.getNumber() + " à fabriquer",
                "order-notification",
                model
        );
    }

    private static List<Map<String, Object>> itemsOf(Order order) {
        return order.getItems().stream().map(item -> {
            Map<String, Object> row = new HashMap<>();
            row.put("name", item.getName());
            row.put("quantity", item.getQuantity());
            row.put("unitPrice", amount(item.getUnitPrice()));
            row.put("lineTotal", amount(item.getLineTotal()));
            return row;
        }).toList();
    }

    /** « 70,90 € » */
    static String amount(BigDecimal amount) {
        return NumberFormat.getCurrencyInstance(Locale.FRANCE).format(amount);
    }

}
