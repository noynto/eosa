package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.UpdateCategoryOfProduct;
import me.noynto.eosa.product.ProductCategory;
import me.noynto.eosa.shared.ProductId;

public record PatchCategoryOfProductHandler(
        UpdateCategoryOfProduct updateCategoryOfProduct,
        String adminId,
        String adminSecret
) implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
        if (!BasicAuth.isAuthorized(ctx.header("Authorization"), adminId, adminSecret)) {
            ctx.status(401);
            return;
        }
        ProductCategory category;
        try {
            category = ProductCategory.valueOf(ctx.formParam("category"));
        } catch (IllegalArgumentException illegalArgumentException) {
            category = null;
        }
        var command = new UpdateCategoryOfProduct.Command(
                new ProductId(ctx.pathParam("product-id")),
                category
        );
        var product = updateCategoryOfProduct.handle(command);
        ctx.status(200).result(product.getId().value());
    }

}