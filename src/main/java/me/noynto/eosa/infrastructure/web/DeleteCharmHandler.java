package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.DeleteCharm;
import me.noynto.eosa.shared.CharmId;

public record DeleteCharmHandler(DeleteCharm deleteCharm) implements Handler {

    @Override
    public void handle(Context ctx) {
        deleteCharm.handle(new DeleteCharm.Command(new CharmId(ctx.pathParam("id"))));
        ctx.header("HX-Redirect", "/admin/charms");
        ctx.status(200);
    }

}
