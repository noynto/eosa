package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadOrders;
import me.noynto.eosa.order.OrderState;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Liste des commandes pour l'administration. Filtre {@code state} : « active » (par défaut, à fabriquer et
 * fabriquées), « all » ou un état ; recherche {@code q} sur le numéro, le nom ou l'e-mail.
 */
public record GetAdminOrdersHandler(
        ReadOrders readOrders
) implements Handler {

    private static final String ACTIVE = "active";
    private static final String ALL = "all";

    @Override
    public void handle(Context ctx) {
        String filter = filterOf(ctx.queryParam("state"));
        String search = ctx.queryParam("q") == null ? "" : ctx.queryParam("q").strip();
        var entries = readOrders.handle(new ReadOrders.Command(statesOf(filter), search)).orders();

        List<Map<String, Object>> rows = entries.stream().map(entry -> {
            var order = entry.order();
            Map<String, Object> row = new HashMap<>();
            row.put("id", order.getId().value());
            row.put("number", order.getNumber());
            row.put("customer", order.getCustomerName() != null ? order.getCustomerName() : order.getCustomerEmail());
            row.put("paidAt", OrderFormat.dateTime(order.getPaidAt()));
            row.put("total", OrderFormat.amount(order.getTotalAmount()));
            row.put("state", OrderFormat.state(order.getState()));
            row.put("overdue", entry.overdue());
            return row;
        }).toList();

        List<Map<String, Object>> filters = new ArrayList<>();
        filters.add(filterOption(ACTIVE, "À traiter", filter));
        for (OrderState state : OrderState.values()) {
            filters.add(filterOption(state.name(), OrderFormat.state(state), filter));
        }
        filters.add(filterOption(ALL, "Toutes", filter));

        Map<String, Object> model = new HashMap<>();
        model.put("title", "Eosa — Commandes");
        model.put("orders", rows);
        model.put("hasOrders", !rows.isEmpty());
        model.put("filters", filters);
        model.put("filter", filter);
        model.put("search", search);
        ctx.render("admin/orders.mustache", model);
    }

    private static String filterOf(String value) {
        if (value == null || value.isBlank() || ACTIVE.equals(value)) {
            return ACTIVE;
        }
        if (ALL.equals(value)) {
            return ALL;
        }
        try {
            return OrderState.valueOf(value).name();
        } catch (IllegalArgumentException e) {
            return ACTIVE;
        }
    }

    private static Set<OrderState> statesOf(String filter) {
        return switch (filter) {
            case ACTIVE -> EnumSet.of(OrderState.PAID, OrderState.CRAFTED);
            case ALL -> null;
            default -> EnumSet.of(OrderState.valueOf(filter));
        };
    }

    private static Map<String, Object> filterOption(String value, String label, String selected) {
        Map<String, Object> option = new HashMap<>();
        option.put("value", value);
        option.put("label", label);
        option.put("selected", value.equals(selected));
        return option;
    }

}
