package me.noynto.eosa.service;

import me.noynto.eosa.mail.Mail;
import me.noynto.eosa.workshop.WorkshopBooking;
import me.noynto.eosa.workshop.WorkshopDates;
import me.noynto.eosa.workshop.WorkshopSession;
import me.noynto.eosa.workshop.WorkshopSessionState;
import me.noynto.eosa.workshop.WorkshopVenue;
import me.noynto.eosa.workshop.WorkshopWaitlistEntry;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Les e-mails d'atelier qui croisent une réservation (ou une inscription en liste d'attente), sa session et son
 * lieu. Chaque méthode construit le {@link Mail} : l'envoi, et l'enregistrement qu'il est parti, restent au
 * cas d'usage.
 */
public final class WorkshopMails {

    private WorkshopMails() {
    }

    /**
     * Récapitulatif d'une réservation confirmée.
     */
    public static Mail bookingConfirmation(WorkshopBooking booking, WorkshopSession session, WorkshopVenue venue, URI publicBaseUrl) {
        String venueAddress = venue.getAddress() + ", " + venue.getPostalCode() + " " + venue.getCity();
        var cancellationDeadline = session.getStartsAt().minus(WorkshopBooking.SELF_CANCELLATION_DEADLINE);
        Map<String, Object> model = new HashMap<>();
        model.put("preheader", "Votre place est réservée : rendez-vous le " + WorkshopDates.dateInSentence(session.getStartsAt())
                + " à " + WorkshopDates.time(session.getStartsAt()) + " chez " + venue.getName() + ".");
        model.put("firstName", booking.getFirstName());
        model.put("venueName", venue.getName());
        model.put("venueAddress", venueAddress);
        model.put("mapUrl", "https://www.google.com/maps/search/?api=1&query=" + URLEncoder.encode(venueAddress, StandardCharsets.UTF_8));
        model.put("date", WorkshopDates.date(session.getStartsAt()));
        model.put("timeRange", WorkshopDates.timeRange(session));
        model.put("duration", WorkshopDates.duration(session.getDurationMinutes()));
        model.put("cancellationDeadline", WorkshopDates.dateInSentence(cancellationDeadline) + " à " + WorkshopDates.time(cancellationDeadline));
        model.put("cancelUrl", publicBaseUrl + "/workshops/bookings/" + booking.getToken() + "/cancel");
        return new Mail(
                booking.getEmail(),
                "Votre place est réservée — Atelier EOSA chez " + venue.getName(),
                "workshop-booking-confirmation",
                model
        );
    }

    /**
     * Annulation d'une réservation. Le message diffère selon que la session entière est annulée ou seulement
     * la réservation.
     */
    public static Mail bookingCancellation(WorkshopBooking booking, WorkshopSession session, WorkshopVenue venue, URI publicBaseUrl) {
        boolean sessionCancelled = session.getState() == WorkshopSessionState.CANCELLED;
        Map<String, Object> model = new HashMap<>();
        model.put("preheader", sessionCancelled
                ? "L'atelier du " + WorkshopDates.dateInSentence(session.getStartsAt()) + " chez " + venue.getName() + " est annulé."
                : "Votre réservation pour l'atelier du " + WorkshopDates.dateInSentence(session.getStartsAt()) + " est annulée.");
        model.put("firstName", booking.getFirstName());
        model.put("sessionCancelled", sessionCancelled);
        model.put("venueName", venue.getName());
        model.put("date", WorkshopDates.date(session.getStartsAt()));
        model.put("timeRange", WorkshopDates.timeRange(session));
        model.put("workshopsUrl", publicBaseUrl + "/workshops");
        return new Mail(
                booking.getEmail(),
                (sessionCancelled ? "Atelier annulé" : "Réservation annulée") + " — Atelier EOSA chez " + venue.getName(),
                "workshop-booking-cancellation",
                model
        );
    }

    /**
     * Annulation de la session, pour une personne qui était en liste d'attente.
     */
    public static Mail waitlistSessionCancellation(WorkshopWaitlistEntry entry, WorkshopSession session, WorkshopVenue venue, URI publicBaseUrl) {
        Map<String, Object> model = new HashMap<>();
        model.put("preheader", "L'atelier du " + WorkshopDates.dateInSentence(session.getStartsAt())
                + " chez " + venue.getName() + " est annulé.");
        model.put("firstName", entry.getFirstName());
        model.put("sessionCancelled", true);
        model.put("waitlisted", true);
        model.put("venueName", venue.getName());
        model.put("date", WorkshopDates.date(session.getStartsAt()));
        model.put("timeRange", WorkshopDates.timeRange(session));
        model.put("workshopsUrl", publicBaseUrl + "/workshops");
        return new Mail(
                entry.getEmail(),
                "Atelier annulé — Atelier EOSA chez " + venue.getName(),
                "workshop-booking-cancellation",
                model
        );
    }

    /**
     * Une place s'est libérée, pour une personne en liste d'attente.
     */
    public static Mail waitlistPlaceAvailable(WorkshopWaitlistEntry entry, WorkshopSession session, WorkshopVenue venue, URI publicBaseUrl) {
        Map<String, Object> model = new HashMap<>();
        model.put("preheader", "Une place vient de se libérer pour l'atelier du "
                + WorkshopDates.dateInSentence(session.getStartsAt()) + " chez " + venue.getName() + ".");
        model.put("firstName", entry.getFirstName());
        model.put("venueName", venue.getName());
        model.put("date", WorkshopDates.date(session.getStartsAt()));
        model.put("timeRange", WorkshopDates.timeRange(session));
        model.put("sessionUrl", publicBaseUrl + "/workshops/sessions/" + session.getId().value());
        model.put("leaveUrl", publicBaseUrl + "/workshops/waitlist/" + entry.getToken() + "/leave");
        return new Mail(
                entry.getEmail(),
                "Une place s'est libérée — Atelier EOSA chez " + venue.getName(),
                "workshop-waitlist-place-available",
                model
        );
    }

}
