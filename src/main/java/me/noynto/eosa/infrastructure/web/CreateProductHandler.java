package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.CreateProduct;

public record CreateProductHandler(
        CreateProduct createProduct,
        String adminId,
        String adminSecret
) implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
        if (!BasicAuth.isAuthorized(ctx.header("Authorization"), adminId, adminSecret)) {
            ctx.status(401);
            return;
        }
        var command = new CreateProduct.Command(
                ctx.formParam("name")
        );
        var product = createProduct.handle(command);
        ctx.status(201).result(product.getId().value());
    }

}