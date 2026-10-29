package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.CreateWorkshopVenue;

public record CreateWorkshopVenueHandler(CreateWorkshopVenue createWorkshopVenue) implements Handler {

    @Override
    public void handle(Context ctx) {
        try {
            var workshopVenue = createWorkshopVenue.handle(new CreateWorkshopVenue.Command(
                    ctx.formParam("name"),
                    ctx.formParam("address"),
                    ctx.formParam("postalCode"),
                    ctx.formParam("city")
            ));
            ctx.header("HX-Redirect", "/workshop-venues/" + workshopVenue.getId().value());
            ctx.status(200);
        } catch (RuntimeException e) {
            ctx.status(422).html("<span class=\"text-red-600 text-xs\">" + e.getMessage() + "</span>");
        }
    }

}
