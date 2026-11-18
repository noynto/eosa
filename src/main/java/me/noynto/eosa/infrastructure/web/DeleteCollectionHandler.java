package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.DeleteCollection;
import me.noynto.eosa.shared.CollectionId;

public record DeleteCollectionHandler(DeleteCollection deleteCollection) implements Handler {

    @Override
    public void handle(Context ctx) {
        try {
            deleteCollection.handle(new DeleteCollection.Command(new CollectionId(ctx.pathParam("id"))));
            ctx.header("HX-Redirect", "/collections");
            ctx.status(200);
        } catch (RuntimeException e) {
            InlineError.render(ctx, e);
        }
    }

}
