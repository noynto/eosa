package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadDashboardMonth;

import java.util.HashMap;
import java.util.Map;

/** Bloc « mois en cours » du tableau de bord : chiffre d'affaires, commandes, panier moyen, remises. */
public record GetAdminDashboardMonthHandler(ReadDashboardMonth readDashboardMonth) implements Handler {

    @Override
    public void handle(Context ctx) {
        var month = readDashboardMonth.handle();

        Map<String, Object> model = new HashMap<>();
        model.put("month", DashboardFormat.month(month.firstDay()));
        model.put("revenue", OrderFormat.amount(month.revenue()));
        model.put("shipping", OrderFormat.amount(month.shipping()));
        model.put("orders", month.orders());
        model.put("previousOrders", month.previousOrders());
        model.put("averageBasket", OrderFormat.amount(month.averageBasket()));
        model.put("averageItems", OrderFormat.amount(month.averageItems()));
        model.put("hasOrders", month.orders() > 0);
        model.put("discounts", OrderFormat.amount(month.promoDiscount().add(month.giftCardPart())));
        model.put("promoDiscount", OrderFormat.amount(month.promoDiscount()));
        model.put("giftCardPart", OrderFormat.amount(month.giftCardPart()));
        ctx.render("admin/dashboard/month.mustache", model);
    }

}
