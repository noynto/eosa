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

}
