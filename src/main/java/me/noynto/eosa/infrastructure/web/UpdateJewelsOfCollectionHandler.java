package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.UpdateJewelsOfCollection;
import me.noynto.eosa.shared.CollectionId;
import me.noynto.eosa.shared.JewelId;

public record UpdateJewelsOfCollectionHandler(UpdateJewelsOfCollection updateJewelsOfCollection) implements Handler {

    @Override
    public void handle(Context ctx) {
        try {
            updateJewelsOfCollection.handle(new UpdateJewelsOfCollection.Command(
                    new CollectionId(ctx.pathParam("id")),
                    ctx.formParams("jewelIds").stream().map(JewelId::new).toList()
            ));
            ctx.html("<span class=\"text-success text-xs\">Sauvegardé</span>");
        } catch (RuntimeException e) {
            InlineError.render(ctx, e);
        }
    }

}
