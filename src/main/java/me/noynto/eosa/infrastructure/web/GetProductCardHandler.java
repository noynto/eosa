package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadProduct;
import me.noynto.eosa.shared.ProductId;

import java.util.Map;

public record GetProductCardHandler(ReadProduct readProduct) implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
        try {
            var product = readProduct.handle(new ReadProduct.Command(new ProductId(ctx.pathParam("id"))));
            ctx.render("partials/product-card.jte", Map.of("product", product));
        } catch (RuntimeException e) {
            ctx.status(404);
        }
    }

}