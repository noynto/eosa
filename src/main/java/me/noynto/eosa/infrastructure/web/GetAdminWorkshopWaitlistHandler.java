package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadWorkshopWaitlist;
import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.workshop.WorkshopSession;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Fragment de la page d'une session pour l'administration : la section « Liste d'attente », vide s'il n'y a
 * personne en attente.
 */
public record GetAdminWorkshopWaitlistHandler(ReadWorkshopWaitlist readWorkshopWaitlist) implements Handler {

    private static final DateTimeFormatter JOINED_AT = DateTimeFormatter.ofPattern("d MMM à H 'h' mm", Locale.FRENCH);

    @Override
    public void handle(Context ctx) {
        List<Map<String, Object>> waitlist;
        try {
            waitlist = readWorkshopWaitlist.handle(new ReadWorkshopWaitlist.Command(new WorkshopSessionId(ctx.pathParam("id")))).entries().stream()
                    .map(entry -> {
                        Map<String, Object> row = new HashMap<>();
                        row.put("firstName", entry.getFirstName());
                        row.put("email", entry.getEmail());
                        row.put("joinedAt", JOINED_AT.format(entry.getCreatedAt().atZone(WorkshopSession.ZONE)));
                        row.put("notified", entry.getLastNotifiedAt() != null);
                        return row;
                    })
                    .toList();
        } catch (RuntimeException e) {
            ctx.status(404);
            return;
        }
        Map<String, Object> model = new HashMap<>();
        model.put("waitlist", waitlist);
        model.put("hasWaitlist", !waitlist.isEmpty());
        ctx.render("admin/partials/workshop-waitlist.mustache", model);
    }

}
