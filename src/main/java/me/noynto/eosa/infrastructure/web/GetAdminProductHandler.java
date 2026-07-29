package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadProduct;
import me.noynto.eosa.product.ProductCategory;
import me.noynto.eosa.product.ProductState;
import me.noynto.eosa.shared.ProductId;

import java.util.HashMap;
import java.util.Map;

public record GetAdminProductHandler(ReadProduct readProduct) implements Handler {

    @Override
    public void handle(Context ctx) {
        try {
            var product = readProduct.handle(new ReadProduct.Command(new ProductId(ctx.pathParam("id"))));
            Map<String, Object> model = new HashMap<>();
            model.put("title", product.getName());
            model.put("name", product.getName());
            model.put("productId", product.getId().value());
            model.put("tagline", product.getTagline() != null ? product.getTagline() : "");
            model.put("price", product.getPrice() != null ? product.getPrice().toPlainString() : "");
            model.put("categoryNecklaceSelected", product.getCategory() == ProductCategory.NECKLACE);
            model.put("categoryBraceletSelected", product.getCategory() == ProductCategory.BRACELET);
            model.put("stateDraftedSelected", product.getState() == ProductState.DRAFTED);
            model.put("statePublishedSelected", product.getState() == ProductState.PUBLISHED);
            model.put("stateArchivedSelected", product.getState() == ProductState.ARCHIVED);
            model.put("hasImages", !product.getImageIds().isEmpty());
            model.put("images", product.getImageIds().stream().map(id -> Map.of("id", id.value())).toList());
            ctx.render("admin/product.mustache", model);
        } catch (RuntimeException e) {
            ctx.status(404);
        }
    }

}