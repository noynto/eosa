package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadDashboardTodo;

import java.util.HashMap;
import java.util.Map;

/** Bloc « À faire » du tableau de bord : fabriquer, expédier, suivre. */
public record GetAdminDashboardTodoHandler(ReadDashboardTodo readDashboardTodo) implements Handler {

    @Override
    public void handle(Context ctx) {
        var todo = readDashboardTodo.handle();

        Map<String, Object> model = new HashMap<>();
        model.put("toCraft", todo.toCraft());
        model.put("toCraftLabel", DashboardFormat.plural(todo.toCraft(), "Pièce à fabriquer", "Pièces à fabriquer"));
        model.put("hasToCraft", todo.toCraft() > 0);
        model.put("craftOverdue", todo.craftOverdue());
        model.put("hasCraftOverdue", todo.craftOverdue() > 0);
        model.put("craftDeadline", todo.nextCraftDeadline() == null ? "" : DashboardFormat.day(todo.nextCraftDeadline()));

        model.put("toShip", todo.toShip());
        model.put("toShipLabel", DashboardFormat.plural(todo.toShip(), "Colis à expédier ou remettre", "Colis à expédier ou remettre"));
        model.put("hasToShip", todo.toShip() > 0);
        model.put("shipOverdue", todo.shipOverdue());
        model.put("hasShipOverdue", todo.shipOverdue() > 0);

        model.put("inTransit", todo.inTransit());
        model.put("inTransitLabel", DashboardFormat.plural(todo.inTransit(), "Colis en route", "Colis en route"));
        model.put("hasInTransit", todo.inTransit() > 0);
        ctx.render("admin/dashboard/todo.mustache", model);
    }

}
