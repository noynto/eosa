package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.CreateProduct;
import me.noynto.eosa.application.CreateVariant;
import me.noynto.eosa.application.SetDefaultVariantOfProduct;
import me.noynto.eosa.shared.IdentityId;

import java.util.Map;

public record CreateProductHandler(
        CreateProduct createProduct,
        CreateVariant createVariant,
        SetDefaultVariantOfProduct setDefaultVariantOfProduct
) implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
        IdentityId identityId = ctx.attribute("identityId");
        var product = createProduct.handle(new CreateProduct.Command(identityId, ctx.formParam("name")));

        var withVariant = createVariant.handle(new CreateVariant.Command(product.getId(), Map.of()));
        var variant = withVariant.getVariants().getLast();
        setDefaultVariantOfProduct.handle(new SetDefaultVariantOfProduct.Command(product.getId(), variant.getId()));

        ctx.header("HX-Redirect", "/admin/products/" + product.getId().value());
        ctx.status(200);
    }

}
