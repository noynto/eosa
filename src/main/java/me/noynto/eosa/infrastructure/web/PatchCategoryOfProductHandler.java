package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.UpdateCategoryOfProduct;
import me.noynto.eosa.product.ProductCategory;
import me.noynto.eosa.shared.ProductId;

public record PatchCategoryOfProductHandler(UpdateCategoryOfProduct updateCategoryOfProduct) implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
        ProductCategory category;
        try {
            category = ProductCategory.valueOf(ctx.formParam("category"));
        } catch (IllegalArgumentException e) {
            category = null;
        }
        try {
            updateCategoryOfProduct.handle(new UpdateCategoryOfProduct.Command(
                    new ProductId(ctx.pathParam("product-id")),
                    category
            ));
            ctx.html("<span class=\"text-success text-xs\">Sauvegardé</span>");
        } catch (RuntimeException e) {
            ctx.status(422).html("<span class=\"text-red-600 text-xs\">" + e.getMessage() + "</span>");
        }
    }

}