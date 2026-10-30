package me.noynto.eosa.application;

import me.noynto.eosa.mail.Mail;
import me.noynto.eosa.mail.MailProvider;
import me.noynto.eosa.shared.WorkshopBookingId;
import me.noynto.eosa.workshop.WorkshopBooking;
import me.noynto.eosa.workshop.WorkshopBookingProvider;
import me.noynto.eosa.workshop.WorkshopBookingState;
import me.noynto.eosa.workshop.WorkshopDates;
import me.noynto.eosa.workshop.WorkshopSession;
import me.noynto.eosa.workshop.WorkshopSessionProvider;
import me.noynto.eosa.workshop.WorkshopSessionState;
import me.noynto.eosa.workshop.WorkshopVenue;
import me.noynto.eosa.workshop.WorkshopVenueProvider;

import java.net.URI;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

/**
 * Envoie l'e-mail d'annulation d'une réservation annulée, s'il n'est pas déjà parti. Le message diffère
 * selon que c'est la session entière qui est annulée ou seulement la réservation.
 */
public record SendWorkshopBookingCancellation(
        WorkshopBookingProvider workshopBookingProvider,
        WorkshopSessionProvider workshopSessionProvider,
        WorkshopVenueProvider workshopVenueProvider,
        MailProvider mailProvider,
        Clock clock,
        URI publicBaseUrl
) {

    public void handle(Command command) {
        if (command.workshopBookingId == null || command.workshopBookingId.value() == null) {
            throw new RuntimeException("L'identifiant de la réservation est nécessaire.");
        }
        WorkshopBooking workshopBooking = workshopBookingProvider.read(command.workshopBookingId)
                .orElseThrow(() -> new RuntimeException("La réservation " + command.workshopBookingId.value() + " n'existe pas."));
        if (workshopBooking.getState() != WorkshopBookingState.CANCELLED || workshopBooking.getCancellationEmailSentAt() != null) {
            return;
        }
        WorkshopSession workshopSession = workshopSessionProvider.read(workshopBooking.getSessionId()).orElseThrow();
        WorkshopVenue workshopVenue = workshopVenueProvider.read(workshopSession.getVenueId()).orElseThrow();
        boolean sessionCancelled = workshopSession.getState() == WorkshopSessionState.CANCELLED;

        Map<String, Object> model = new HashMap<>();
        model.put("preheader", sessionCancelled
                ? "L'atelier du " + WorkshopDates.dateInSentence(workshopSession.getStartsAt()) + " chez " + workshopVenue.getName() + " est annulé."
                : "Votre réservation pour l'atelier du " + WorkshopDates.dateInSentence(workshopSession.getStartsAt()) + " est annulée.");
        model.put("firstName", workshopBooking.getFirstName());
        model.put("sessionCancelled", sessionCancelled);
        model.put("venueName", workshopVenue.getName());
        model.put("date", WorkshopDates.date(workshopSession.getStartsAt()));
        model.put("timeRange", WorkshopDates.timeRange(workshopSession));
        model.put("workshopsUrl", publicBaseUrl + "/workshops");

        mailProvider.send(new Mail(
                workshopBooking.getEmail(),
                (sessionCancelled ? "Atelier annulé" : "Réservation annulée") + " — Atelier EOSA chez " + workshopVenue.getName(),
                "workshop-booking-cancellation",
                model
        ));
        workshopBooking.setCancellationEmailSentAt(clock.instant());
        workshopBookingProvider.write(workshopBooking);
    }

    public record Command(
            WorkshopBookingId workshopBookingId
    ) {
    }

}
