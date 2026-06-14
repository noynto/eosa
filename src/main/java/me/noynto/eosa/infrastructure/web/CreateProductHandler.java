package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.CreateProduct;
import me.noynto.eosa.shared.IdentityId;

public record CreateProductHandler(CreateProduct createProduct) implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
        IdentityId identityId = ctx.attribute("identityId");
        var product = createProduct.handle(new CreateProduct.Command(identityId, ctx.formParam("name")));
        ctx.header("HX-Redirect", "/admin/products/" + product.getId().value());
        ctx.status(200);
    }

}