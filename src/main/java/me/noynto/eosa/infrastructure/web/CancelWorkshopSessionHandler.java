package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.CancelWorkshopSession;
import me.noynto.eosa.shared.WorkshopSessionId;

public record CancelWorkshopSessionHandler(CancelWorkshopSession cancelWorkshopSession) implements Handler {

    @Override
    public void handle(Context ctx) {
        try {
            var workshopSession = cancelWorkshopSession.handle(
                    new CancelWorkshopSession.Command(new WorkshopSessionId(ctx.pathParam("id")))
            ).session();
            ctx.header("HX-Redirect", "/workshops/" + workshopSession.getId().value());
            ctx.status(200);
        } catch (RuntimeException e) {
            InlineError.render(ctx, e);
        }
    }

}
