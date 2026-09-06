package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.CreateMetalColor;

public record CreateMetalColorHandler(CreateMetalColor createMetalColor) implements Handler {

    @Override
    public void handle(Context ctx) {
        createMetalColor.handle(new CreateMetalColor.Command(ctx.formParam("name")));
        ctx.header("HX-Redirect", "/admin/metal-colors");
        ctx.status(200);
    }

}
