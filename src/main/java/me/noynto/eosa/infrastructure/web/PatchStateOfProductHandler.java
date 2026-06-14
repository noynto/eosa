package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.UpdateStateOfProduct;
import me.noynto.eosa.product.ProductState;
import me.noynto.eosa.shared.ProductId;

public record PatchStateOfProductHandler(UpdateStateOfProduct updateStateOfProduct) implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
        ProductState state;
        try {
            state = ProductState.valueOf(ctx.formParam("state"));
        } catch (IllegalArgumentException e) {
            state = null;
        }
        try {
            updateStateOfProduct.handle(new UpdateStateOfProduct.Command(
                    new ProductId(ctx.pathParam("product-id")),
                    state
            ));
            ctx.html("<span class=\"text-success text-xs\">Sauvegardé</span>");
        } catch (RuntimeException e) {
            ctx.status(422).html("<span class=\"text-red-600 text-xs\">" + e.getMessage() + "</span>");
        }
    }

}