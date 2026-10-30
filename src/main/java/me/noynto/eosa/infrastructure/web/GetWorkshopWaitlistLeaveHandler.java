package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadWorkshopWaitlistEntry;
import me.noynto.eosa.workshop.WorkshopDates;

import java.util.HashMap;
import java.util.Map;

/**
 * Page du lien « Quitter la liste d'attente ». Un simple affichage ne désinscrit pas (les messageries
 * ouvrent parfois les liens d'elles-mêmes) : la désinscription se fait avec le bouton, en POST.
 */
public record GetWorkshopWaitlistLeaveHandler(
        ReadWorkshopWaitlistEntry readWorkshopWaitlistEntry,
        String baseUrl
) implements Handler {

    @Override
    public void handle(Context ctx) {
        ReadWorkshopWaitlistEntry.Overview overview;
        try {
            overview = readWorkshopWaitlistEntry.handle(new ReadWorkshopWaitlistEntry.Command(ctx.pathParam("token")));
        } catch (RuntimeException e) {
            ctx.status(404);
            return;
        }
        Map<String, Object> model = new HashMap<>();
        model.put("title", "Eosa — Liste d'attente");
        model.put("description", "Votre inscription en liste d'attente pour un atelier EOSA.");
        model.put("ogImageUrl", baseUrl + "/workshops.webp");
        model.put("canonicalUrl", baseUrl + "/workshops");
        model.put("noindex", true);
        model.put("token", overview.entry().getToken());
        model.put("firstName", overview.entry().getFirstName());
        model.put("waiting", overview.entry().isWaiting());
        model.put("venueName", overview.venue().getName());
        model.put("date", WorkshopDates.date(overview.session().getStartsAt()));
        model.put("timeRange", WorkshopDates.timeRange(overview.session()));
        model.put("sessionId", overview.session().getId().value());
        ctx.render("workshop-waitlist-leave.mustache", model);
    }

}
