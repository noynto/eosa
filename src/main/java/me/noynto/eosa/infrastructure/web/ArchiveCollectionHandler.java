package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ArchiveCollection;
import me.noynto.eosa.shared.CollectionId;

public record ArchiveCollectionHandler(ArchiveCollection archiveCollection) implements Handler {

    @Override
    public void handle(Context ctx) {
        try {
            archiveCollection.handle(new ArchiveCollection.Command(new CollectionId(ctx.pathParam("id"))));
            ctx.header("HX-Redirect", "/collections/" + ctx.pathParam("id"));
            ctx.status(200);
        } catch (RuntimeException e) {
            InlineError.render(ctx, e);
        }
    }

}
