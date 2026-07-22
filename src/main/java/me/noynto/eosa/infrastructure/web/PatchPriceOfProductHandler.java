package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadProduct;
import me.noynto.eosa.application.UpdatePriceOfVariant;
import me.noynto.eosa.shared.ProductId;

import java.math.BigDecimal;

public record PatchPriceOfProductHandler(
        ReadProduct readProduct,
        UpdatePriceOfVariant updatePriceOfVariant
) implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
        BigDecimal price;
        try {
            price = new BigDecimal(ctx.formParam("price"));
        } catch (NumberFormatException e) {
            price = null;
        }
        try {
            var productId = new ProductId(ctx.pathParam("product-id"));
            var product = readProduct.handle(new ReadProduct.Command(productId));
            var variant = DefaultVariants.resolve(product);

            updatePriceOfVariant.handle(new UpdatePriceOfVariant.Command(productId, variant.getId(), price));
            ctx.html("<span class=\"text-success text-xs\">Sauvegardé</span>");
        } catch (RuntimeException e) {
            ctx.status(422).html("<span class=\"text-red-600 text-xs\">" + e.getMessage() + "</span>");
        }
    }

}
