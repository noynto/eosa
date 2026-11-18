package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.UpdateCollection;
import me.noynto.eosa.shared.CollectionId;

public record UpdateCollectionHandler(UpdateCollection updateCollection) implements Handler {

    @Override
    public void handle(Context ctx) {
        try {
            updateCollection.handle(new UpdateCollection.Command(
                    new CollectionId(ctx.pathParam("id")),
                    ctx.formParam("name"),
                    CollectionLabels.parseSeason(ctx.formParam("season")),
                    CollectionLabels.parseYear(ctx.formParam("year"))
            ));
            ctx.html("<span class=\"text-success text-xs\">Sauvegardé</span>");
        } catch (RuntimeException e) {
            InlineError.render(ctx, e);
        }
    }

}
