package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.UpdatePriceOfProduct;
import me.noynto.eosa.application.UpdateStateOfProduct;
import me.noynto.eosa.product.ProductState;
import me.noynto.eosa.shared.ProductId;

import java.math.BigDecimal;

public record PatchPriceOfProductHandler(
        UpdatePriceOfProduct updatePriceOfProduct,
        String adminId,
        String adminSecret
) implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
        if (!BasicAuth.isAuthorized(ctx.header("Authorization"), adminId, adminSecret)) {
            ctx.status(401);
            return;
        }
        BigDecimal price;
        try {
            price = new BigDecimal(ctx.formParam("price"));
        } catch (NumberFormatException numberFormatException) {
            price = null;
        }
        var command = new UpdatePriceOfProduct.Command(
                new ProductId(ctx.pathParam("product-id")),
                price
        );
        var product = updatePriceOfProduct.handle(command);
        ctx.status(200).result(product.getId().value());
    }

}