package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.CreateProduct;
import me.noynto.eosa.application.UpdateTaglineOfProduct;
import me.noynto.eosa.shared.ProductId;

public record PatchTaglineOfProductHandler(
        UpdateTaglineOfProduct updateTaglineOfProduct,
        String adminId,
        String adminSecret
) implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
        if (!BasicAuth.isAuthorized(ctx.header("Authorization"), adminId, adminSecret)) {
            ctx.status(401);
            return;
        }
        var command = new UpdateTaglineOfProduct.Command(
                new ProductId(ctx.pathParam("product-id")),
                ctx.formParam("tagline")
        );
        var product = updateTaglineOfProduct.handle(command);
        ctx.status(200).result(product.getId().value());
    }

}