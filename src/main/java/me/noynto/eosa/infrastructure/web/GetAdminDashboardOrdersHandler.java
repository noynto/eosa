package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadDashboardLatestOrders;
import me.noynto.eosa.order.OrderState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Bloc « Dernières commandes » du tableau de bord. */
public record GetAdminDashboardOrdersHandler(ReadDashboardLatestOrders readDashboardLatestOrders) implements Handler {

    @Override
    public void handle(Context ctx) {
        List<Map<String, Object>> rows = readDashboardLatestOrders.handle().orders().stream().map(order -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id", order.getId().value());
            row.put("number", order.getNumber());
            boolean hasCustomer = order.getCustomerName() != null && !order.getCustomerName().isBlank();
            row.put("hasCustomer", hasCustomer);
            row.put("customer", hasCustomer ? order.getCustomerName() : "");
            row.put("state", OrderFormat.state(order.getState()));
            row.put("delivered", order.getState() == OrderState.DELIVERED);
            row.put("cancelled", order.getState() == OrderState.CANCELLED);
            row.put("total", OrderFormat.amount(order.getTotalAmount()));
            boolean hasCode = order.getPromoCode() != null && order.getDiscountAmount().signum() > 0;
            boolean hasGiftCard = order.getGiftCardAmount().signum() > 0;
            row.put("hasCode", hasCode);
            row.put("code", order.getPromoCode());
            row.put("hasGiftCard", hasGiftCard);
            return row;
        }).toList();

        Map<String, Object> model = new HashMap<>();
        model.put("orders", rows);
        model.put("hasOrders", !rows.isEmpty());
        ctx.render("admin/dashboard/orders.mustache", model);
    }

}
