package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadProduct;
import me.noynto.eosa.product.ProductState;
import me.noynto.eosa.shared.ProductId;

import java.util.HashMap;
import java.util.Map;

public record GetAdminProductRowHandler(ReadProduct readProduct) implements Handler {

    @Override
    public void handle(Context ctx) {
        try {
            var product = readProduct.handle(new ReadProduct.Command(new ProductId(ctx.pathParam("id"))));
            boolean hasState = product.getState() != null;
            boolean hasCategory = product.getCategory() != null;
            Map<String, Object> model = new HashMap<>();
            model.put("productId", product.getId().value());
            model.put("name", product.getName());
            model.put("hasState", hasState);
            model.put("stateLabel", hasState ? product.getState().name().toLowerCase() : "");
            model.put("stateBadgeClass", hasState ? stateBadgeClass(product.getState()) : "");
            model.put("hasCategory", hasCategory);
            model.put("categoryLabel", hasCategory ? product.getCategory().name().toLowerCase() : "");
            ctx.render("admin/partials/product-row.mustache", model);
        } catch (RuntimeException e) {
            ctx.status(404);
        }
    }

    private static String stateBadgeClass(ProductState state) {
        return switch (state) {
            case PUBLISHED -> "bg-success/10 text-success";
            case DRAFTED -> "bg-bg-border text-secondary";
            default -> "bg-bg-border text-muted";
        };
    }

}