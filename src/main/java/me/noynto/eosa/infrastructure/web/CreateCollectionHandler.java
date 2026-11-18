package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.CreateCollection;

public record CreateCollectionHandler(CreateCollection createCollection) implements Handler {

    @Override
    public void handle(Context ctx) {
        try {
            var collection = createCollection.handle(new CreateCollection.Command(
                    ctx.formParam("name"),
                    CollectionLabels.parseSeason(ctx.formParam("season")),
                    CollectionLabels.parseYear(ctx.formParam("year"))
            )).collection();
            ctx.header("HX-Redirect", "/collections/" + collection.getId().value());
            ctx.status(200);
        } catch (RuntimeException e) {
            InlineError.render(ctx, e);
        }
    }

}
