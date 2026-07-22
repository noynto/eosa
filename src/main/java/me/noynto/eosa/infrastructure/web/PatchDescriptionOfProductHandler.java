package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.UpdateDescriptionOfProduct;
import me.noynto.eosa.shared.ProductId;

public record PatchDescriptionOfProductHandler(UpdateDescriptionOfProduct updateDescriptionOfProduct) implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
        try {
            updateDescriptionOfProduct.handle(new UpdateDescriptionOfProduct.Command(
                    new ProductId(ctx.pathParam("product-id")),
                    ctx.formParam("description")
            ));
            ctx.html("<span class=\"text-success text-xs\">Sauvegardé</span>");
        } catch (RuntimeException e) {
            ctx.status(422).html("<span class=\"text-red-600 text-xs\">" + e.getMessage() + "</span>");
        }
    }

}
