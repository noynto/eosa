package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadWorkshopSession;
import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.workshop.WorkshopDates;
import me.noynto.eosa.workshop.WorkshopSession;
import me.noynto.eosa.workshop.WorkshopSessionState;
import me.noynto.eosa.workshop.WorkshopVenue;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Page publique d'une session : le lieu mis en avant, puis le formulaire de réservation quand c'est possible.
 * Les formulaires s'envoient en htmx : la réponse (erreur ou confirmation) vient de {@link PostWorkshopBookingHandler}
 * et {@link PostWorkshopWaitlistHandler}, sans recharger cette page.
 */
public record GetWorkshopSessionHandler(
        ReadWorkshopSession readWorkshopSession,
        Clock clock,
        String baseUrl
) implements Handler {

    @Override
    public void handle(Context ctx) {
        ReadWorkshopSession.Result overview;
        try {
            overview = readWorkshopSession.handle(new ReadWorkshopSession.Command(new WorkshopSessionId(ctx.pathParam("id"))));
        } catch (RuntimeException e) {
            ctx.status(404);
            return;
        }
        var session = overview.session();
        var venue = overview.venue();
        Instant now = clock.instant();
        boolean cancelled = session.getState() == WorkshopSessionState.CANCELLED;
        boolean started = !now.isBefore(session.getStartsAt());
        boolean notOpenYet = !cancelled && !started && now.isBefore(session.getBookingOpensAt());
        int remainingPlaces = Math.max(0, session.getCapacity() - overview.occupiedPlaces());
        boolean full = !cancelled && !started && !notOpenYet && remainingPlaces == 0;
        boolean open = !cancelled && !started && !notOpenYet && remainingPlaces > 0;
        String venueAddress = venue.getAddress() + ", " + venue.getPostalCode() + " " + venue.getCity();
        boolean hasVenueImage = venue.getImageId() != null;

        Map<String, Object> model = new HashMap<>();
        model.put("title", "Eosa — Atelier chez " + venue.getName() + ", " + WorkshopDates.dateInSentence(session.getStartsAt()));
        model.put("description", "Créez votre collier ou bracelet en pierres naturelles lors d'un atelier EOSA chez "
                + venue.getName() + " (" + venue.getCity() + "), le " + WorkshopDates.dateInSentence(session.getStartsAt()) + ".");
        model.put("ogImageUrl", hasVenueImage ? baseUrl + "/images/" + venue.getImageId().value() : baseUrl + "/workshops.webp");
        String sessionUrl = baseUrl + "/workshops/sessions/" + session.getId().value();
        model.put("canonicalUrl", sessionUrl);
        // Indexée tant qu'elle n'est pas terminée : Google peut l'afficher comme évènement, ou comme évènement annulé.
        model.put("noindex", !now.isBefore(session.getEndsAt()));
        model.put("jsonLd", eventJsonLd(session, venue, sessionUrl, (String) model.get("description"),
                (String) model.get("ogImageUrl"), cancelled, open, notOpenYet));
        model.put("sessionId", session.getId().value());
        model.put("venueName", venue.getName());
        model.put("hasVenueDescription", venue.getDescription() != null);
        model.put("venueDescription", venue.getDescription() != null ? venue.getDescription() : "");
        model.put("venueAddress", venueAddress);
        model.put("mapUrl", "https://www.google.com/maps/search/?api=1&query=" + URLEncoder.encode(venueAddress, StandardCharsets.UTF_8));
        model.put("hasVenueLink", venue.getLinkUrl() != null);
        model.put("venueLinkUrl", venue.getLinkUrl() != null ? venue.getLinkUrl() : "");
        model.put("hasVenueImage", hasVenueImage);
        model.put("venueImageId", hasVenueImage ? venue.getImageId().value() : "");
        model.put("date", WorkshopDates.date(session.getStartsAt()));
        model.put("timeRange", WorkshopDates.timeRange(session));
        model.put("duration", WorkshopDates.duration(session.getDurationMinutes()));
        model.put("cancelled", cancelled);
        model.put("started", started && !cancelled);
        model.put("notOpenYet", notOpenYet);
        model.put("bookingOpensAt", WorkshopDates.dateInSentence(session.getBookingOpensAt()) + " à " + WorkshopDates.time(session.getBookingOpensAt()));
        model.put("full", full);
        model.put("open", open);
        model.put("remainingPlaces", remainingPlaces == 1 ? "Dernière place" : remainingPlaces + " places restantes");
        ctx.render("workshop-session.mustache", model);
    }

    /**
     * La session décrite comme un évènement schema.org. Le prix se règle sur place, selon le bijou créé.
     */
    private String eventJsonLd(WorkshopSession session, WorkshopVenue venue, String sessionUrl, String description,
                               String imageUrl, boolean cancelled, boolean open, boolean notOpenYet) {
        String availability = open ? "https://schema.org/InStock"
                : notOpenYet ? "https://schema.org/PreOrder"
                : "https://schema.org/SoldOut";
        String validFrom = session.getBookingOpensAt().atZone(WorkshopSession.ZONE).toOffsetDateTime().toString();
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("@context", "https://schema.org");
        event.put("@type", "Event");
        event.put("name", "Atelier de création de bijoux EOSA chez " + venue.getName());
        event.put("description", description);
        event.put("image", List.of(imageUrl));
        event.put("startDate", session.getStartsAt().atZone(WorkshopSession.ZONE).toOffsetDateTime().toString());
        event.put("endDate", session.getEndsAt().atZone(WorkshopSession.ZONE).toOffsetDateTime().toString());
        event.put("eventStatus", cancelled ? "https://schema.org/EventCancelled" : "https://schema.org/EventScheduled");
        event.put("eventAttendanceMode", "https://schema.org/OfflineEventAttendanceMode");
        event.put("location", Map.of(
                "@type", "Place",
                "name", venue.getName(),
                "address", Map.of(
                        "@type", "PostalAddress",
                        "streetAddress", venue.getAddress(),
                        "postalCode", venue.getPostalCode(),
                        "addressLocality", venue.getCity(),
                        "addressCountry", "FR"
                )
        ));
        event.put("organizer", JsonLd.organization(baseUrl));
        event.put("offers", List.of(
                offer("Collier personnalisé", "33.00", sessionUrl, availability, validFrom),
                offer("Bracelet personnalisé", "23.00", sessionUrl, availability, validFrom)
        ));
        return JsonLd.of(event);
    }

    private static Map<String, Object> offer(String name, String price, String url, String availability, String validFrom) {
        return Map.of(
                "@type", "Offer",
                "name", name,
                "price", price,
                "priceCurrency", "EUR",
                "url", url,
                "availability", availability,
                "validFrom", validFrom
        );
    }

}
