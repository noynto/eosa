package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import me.noynto.eosa.application.AddVariantToCart;
import me.noynto.eosa.application.GetOrCreateCart;
import me.noynto.eosa.application.ReadProduct;
import me.noynto.eosa.shared.CartId;
import me.noynto.eosa.shared.ProductId;

public record PostCartItemHandler(
        GetOrCreateCart getOrCreateCart,
        ReadProduct readProduct,
        AddVariantToCart addVariantToCart
) implements Handler {

    @Override
    public void handle(Context ctx) {
        String cookieValue = ctx.cookie("cart");
        var cart = getOrCreateCart.handle(new GetOrCreateCart.Command(
                cookieValue != null ? new CartId(cookieValue) : null
        ));
        ctx.cookie("cart", cart.getId().value());

        var productId = new ProductId(ctx.pathParam("product-id"));
        var product = readProduct.handle(new ReadProduct.Command(productId));
        var variant = DefaultVariants.resolve(product);

        addVariantToCart.handle(new AddVariantToCart.Command(cart.getId(), productId, variant.getId()));
        redirectOrHtmx(ctx);
    }

    private void redirectOrHtmx(Context ctx) {
        if (ctx.header("HX-Request") != null) {
            ctx.header("HX-Redirect", "/cart");
            ctx.status(HttpStatus.NO_CONTENT);
        } else {
            ctx.redirect("/cart", HttpStatus.SEE_OTHER);
        }
    }

}
