package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.CreateJewel;
import me.noynto.eosa.shared.IdentityId;

public record CreateJewelHandler(CreateJewel createJewel) implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
        IdentityId identityId = ctx.attribute("identityId");
        var jewel = createJewel.handle(new CreateJewel.Command(identityId, ctx.formParam("name")));
        ctx.header("HX-Redirect", "/admin/jewels/" + jewel.getId().value());
        ctx.status(200);
    }

}