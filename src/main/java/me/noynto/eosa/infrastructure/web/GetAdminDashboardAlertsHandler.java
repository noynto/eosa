package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadDashboardAlerts;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bandeau d'alerte du tableau de bord : rien à afficher (204) quand tout va bien, donc pas de bandeau du tout.
 */
public record GetAdminDashboardAlertsHandler(ReadDashboardAlerts readDashboardAlerts) implements Handler {

    @Override
    public void handle(Context ctx) {
        var alerts = readDashboardAlerts.handle();
        if (alerts.isEmpty()) {
            ctx.status(204);
            return;
        }
        List<Map<String, Object>> orders = alerts.orders().stream().map(pending -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id", pending.orderId());
            row.put("number", pending.number());
            return row;
        }).toList();
        List<Map<String, Object>> giftCards = alerts.giftCards().stream().map(id -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id", id.value());
            return row;
        }).toList();

        Map<String, Object> model = new HashMap<>();
        model.put("orders", orders);
        model.put("hasOrders", !orders.isEmpty());
        model.put("ordersLabel", DashboardFormat.plural(orders.size(),
                "e-mail de commande n'est pas parti", "e-mails de commande ne sont pas partis"));
        model.put("orderCount", orders.size());
        model.put("giftCards", giftCards);
        model.put("hasGiftCards", !giftCards.isEmpty());
        model.put("giftCardsLabel", DashboardFormat.plural(giftCards.size(),
                "e-mail de carte cadeau n'est pas parti", "e-mails de carte cadeau ne sont pas partis"));
        model.put("giftCardCount", giftCards.size());
        ctx.render("admin/dashboard/alerts.mustache", model);
    }

}
