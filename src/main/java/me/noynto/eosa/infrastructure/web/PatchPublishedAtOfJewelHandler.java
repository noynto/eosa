package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.UpdatePublishedAtOfJewel;
import me.noynto.eosa.shared.JewelId;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public record PatchPublishedAtOfJewelHandler(UpdatePublishedAtOfJewel updatePublishedAtOfJewel) implements Handler {

    @Override
    public void handle(Context ctx) {
        try {
            updatePublishedAtOfJewel.handle(new UpdatePublishedAtOfJewel.Command(
                    new JewelId(ctx.pathParam("jewel-id")),
                    date(ctx.formParam("publishedOn"))
            ));
            ctx.html("<span class=\"text-success text-xs\">Sauvegardé</span>");
        } catch (RuntimeException e) {
            InlineError.render(ctx, e);
        }
    }

    private static LocalDate date(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.strip());
        } catch (DateTimeParseException e) {
            throw new RuntimeException("La date de publication n'est pas une date valide.");
        }
    }

}
