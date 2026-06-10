package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.CreateProduct;
import me.noynto.eosa.shared.IdentityId;

public record CreateProductHandler(
        CreateProduct createProduct
) implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
        IdentityId identityId = ctx.attribute("identityId");
        var command = new CreateProduct.Command(
                identityId,
                ctx.formParam("name")
        );
        var product = createProduct.handle(command);
        ctx.status(201).result(product.getId().value());
    }

}