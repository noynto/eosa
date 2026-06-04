package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.UpdateStateOfProduct;
import me.noynto.eosa.product.ProductState;
import me.noynto.eosa.shared.ProductId;

public record PatchStateOfProductHandler(
        UpdateStateOfProduct updateStateOfProduct,
        String adminId,
        String adminSecret
) implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
        if (!BasicAuth.isAuthorized(ctx.header("Authorization"), adminId, adminSecret)) {
            ctx.status(401);
            return;
        }
        ProductState state;
        try {
            state = ProductState.valueOf(ctx.formParam("state"));
        } catch (IllegalArgumentException illegalArgumentException) {
            state = null;
        }
        var command = new UpdateStateOfProduct.Command(
                new ProductId(ctx.pathParam("product-id")),
                state
        );
        var product = updateStateOfProduct.handle(command);
        ctx.status(200).result(product.getId().value());
    }

}