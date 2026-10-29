package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.AddImageToWorkshopVenue;
import me.noynto.eosa.image.Image;
import me.noynto.eosa.shared.WorkshopVenueId;

public record AddImageToWorkshopVenueHandler(AddImageToWorkshopVenue addImageToWorkshopVenue) implements Handler {

    @Override
    public void handle(Context ctx) {
        var uploadedFile = ctx.uploadedFile("image");
        try {
            if (uploadedFile == null) {
                throw new RuntimeException("Une photo est nécessaire.");
            }
            Image image = new Image();
            image.setName(uploadedFile.filename());
            image.setFormat(uploadedFile.contentType());
            image.setContent(uploadedFile.content());
            var workshopVenue = addImageToWorkshopVenue.handle(new AddImageToWorkshopVenue.Command(
                    new WorkshopVenueId(ctx.pathParam("id")),
                    image
            ));
            ctx.header("HX-Redirect", "/workshop-venues/" + workshopVenue.getId().value());
            ctx.status(200);
        } catch (RuntimeException e) {
            ctx.status(422).html("<span class=\"text-red-600 text-xs\">" + e.getMessage() + "</span>");
        }
    }

}
