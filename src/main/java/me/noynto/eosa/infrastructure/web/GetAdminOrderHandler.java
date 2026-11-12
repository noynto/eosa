package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadOrder;
import me.noynto.eosa.order.OrderState;
import me.noynto.eosa.shared.OrderId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Détail d'une commande pour l'administration : client, adresse de livraison, lignes et montants.
 */
public record GetAdminOrderHandler(
        ReadOrder readOrder
) implements Handler {

    @Override
    public void handle(Context ctx) {
        ReadOrder.Result result;
        try {
            result = readOrder.handle(new ReadOrder.Command(new OrderId(ctx.pathParam("id"))));
        } catch (RuntimeException e) {
            ctx.status(404);
            return;
        }
        var order = result.order();

        Map<String, Object> model = new HashMap<>();
        model.put("title", "Commande " + order.getNumber());
        model.put("number", order.getNumber());
        model.put("state", OrderFormat.state(order.getState()));
        model.put("paidAt", OrderFormat.dateTime(order.getPaidAt()));
        model.put("showCraftingDeadline", order.getState() == OrderState.PAID);
        model.put("craftingDeadline", OrderFormat.dateTime(result.craftingDeadline()));
        model.put("overdue", result.overdue());
        model.put("showShippingDeadline", order.getState() == OrderState.CRAFTED && !order.isHandDelivery()
                && result.shippingDeadline() != null);
        model.put("isHandDelivery", order.isHandDelivery());
        model.put("canHandOver", order.getState() == OrderState.CRAFTED && order.isHandDelivery());
        model.put("craftedHandAt", order.isHandDelivery() && order.getCraftedAt() != null
                ? OrderFormat.dateTime(order.getCraftedAt()) : null);
        // Vide tant que la commande n'est pas fabriquée : rien à formater alors.
        model.put("shippingDeadline", result.shippingDeadline() == null ? null : OrderFormat.dateTime(result.shippingDeadline()));
        model.put("id", order.getId().value());
        // Fabriquer : seulement une commande à fabriquer ; l'étiquette s'imprime une fois créée.
        boolean toCraft = order.getState() == OrderState.PAID && order.getLabel() == null && !order.isHandDelivery();
        model.put("canCraft", toCraft);
        // L'envoi par transporteur a besoin de l'adresse ; la remise en main propre n'en a pas besoin.
        model.put("canCraftForShipping", toCraft && order.getShippingAddress() != null);
        model.put("defaultWeight", result.defaultParcelWeightGrams());
        var label = order.getLabel();
        model.put("hasLabel", label != null);
        if (label != null) {
            model.put("trackingNumber", label.getTrackingNumber());
            model.put("labelWeight", label.getWeightGrams());
            model.put("craftedAt", OrderFormat.dateTime(order.getCraftedAt()));
            model.put("canPrintLabel", order.getState() == OrderState.CRAFTED || order.getState() == OrderState.SHIPPED);
        }
        // Les actions de la suite : remettre, livrer, annuler (le bouton ne s'affiche que si l'état le permet).
        model.put("canShip", order.getState() == OrderState.CRAFTED && label != null);
        model.put("canDeliver", order.getState() == OrderState.SHIPPED);
        model.put("canCancel", order.getState() == OrderState.PAID || order.getState() == OrderState.CRAFTED
                || order.getState() == OrderState.SHIPPED);
        model.put("hasSuite", order.getShippedAt() != null || order.getDeliveredAt() != null);
        model.put("hasShippedAt", order.getShippedAt() != null);
        model.put("shippedAt", order.getShippedAt() == null ? null : OrderFormat.dateTime(order.getShippedAt()));
        model.put("hasDeliveredAt", order.getDeliveredAt() != null);
        model.put("deliveredAt", order.getDeliveredAt() == null ? null : OrderFormat.dateTime(order.getDeliveredAt()));
        model.put("isCancelled", order.getState() == OrderState.CANCELLED);
        model.put("cancelledAt", order.getCancelledAt() == null ? null : OrderFormat.dateTime(order.getCancelledAt()));
        model.put("cancelReason", order.getCancelReason());
        // Journal des e-mails : ce qui est parti, et ce qui reste à envoyer.
        model.put("confirmationMail", mailStatus(order.getConfirmationSentAt()));
        model.put("notificationMail", mailStatus(order.getNotificationSentAt()));
        model.put("showShippingMail", order.getShippedAt() != null);
        model.put("shippingMail", mailStatus(order.getShippingSentAt()));
        model.put("hasPurgedAt", order.getPersonalDataPurgedAt() != null);
        model.put("purgedAt", order.getPersonalDataPurgedAt() == null ? null : OrderFormat.dateTime(order.getPersonalDataPurgedAt()));
        model.put("customerName", order.getCustomerName());
        model.put("customerEmail", order.getCustomerEmail());
        model.put("hasPhone", order.getCustomerPhone() != null);
        model.put("customerPhone", order.getCustomerPhone());
        model.put("stripeSessionId", order.getStripeSessionId().getStripe());

        // Absente quand la commande est ancienne et que ses données personnelles ont été purgées.
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

        List<Map<String, Object>> items = order.getItems().stream().map(item -> {
            Map<String, Object> row = new HashMap<>();
            row.put("name", item.getName());
            row.put("quantity", item.getQuantity());
            row.put("unitPrice", OrderFormat.amount(item.getUnitPrice()));
            row.put("lineTotal", OrderFormat.amount(item.getLineTotal()));
            row.put("hasImage", item.getImageId() != null);
            row.put("imageId", item.getImageId() != null ? item.getImageId().value() : null);
            row.put("hasJewel", item.getJewelId() != null);
            row.put("jewelId", item.getJewelId() != null ? item.getJewelId().value() : null);
            return row;
        }).toList();
        model.put("items", items);
        model.put("itemsAmount", OrderFormat.amount(order.getItemsAmount()));
        model.put("shippingAmount", OrderFormat.amount(order.getShippingAmount()));
        model.put("totalAmount", OrderFormat.amount(order.getTotalAmount()));
        ctx.render("admin/order.mustache", model);
    }

    /** « Envoyé le 12 nov. 2026 à 10 h 15 » ou « Pas encore envoyé ». */
    private static String mailStatus(java.time.Instant sentAt) {
        return sentAt == null ? "Pas encore envoyé" : "Envoyé le " + OrderFormat.dateTime(sentAt);
    }

}
