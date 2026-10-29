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
import me.noynto.eosa.workshop.WorkshopVenue;
import me.noynto.eosa.workshop.WorkshopVenueProvider;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

/**
 * Envoie l'e-mail récapitulatif d'une réservation confirmée, s'il n'est pas déjà parti.
 */
public record SendWorkshopBookingConfirmation(
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
        if (workshopBooking.getState() != WorkshopBookingState.CONFIRMED || workshopBooking.getConfirmationEmailSentAt() != null) {
            return;
        }
        WorkshopSession workshopSession = workshopSessionProvider.read(workshopBooking.getSessionId()).orElseThrow();
        WorkshopVenue workshopVenue = workshopVenueProvider.read(workshopSession.getVenueId()).orElseThrow();

        String venueAddress = workshopVenue.getAddress() + ", " + workshopVenue.getPostalCode() + " " + workshopVenue.getCity();
        var cancellationDeadline = workshopSession.getStartsAt().minus(WorkshopBooking.SELF_CANCELLATION_DEADLINE);
        Map<String, Object> model = new HashMap<>();
        model.put("preheader", "Votre place est réservée : rendez-vous le " + WorkshopDates.dateInSentence(workshopSession.getStartsAt())
                + " à " + WorkshopDates.time(workshopSession.getStartsAt()) + " chez " + workshopVenue.getName() + ".");
        model.put("firstName", workshopBooking.getFirstName());
        model.put("venueName", workshopVenue.getName());
        model.put("venueAddress", venueAddress);
        model.put("mapUrl", "https://www.google.com/maps/search/?api=1&query=" + URLEncoder.encode(venueAddress, StandardCharsets.UTF_8));
        model.put("date", WorkshopDates.date(workshopSession.getStartsAt()));
        model.put("timeRange", WorkshopDates.timeRange(workshopSession));
        model.put("duration", WorkshopDates.duration(workshopSession.getDurationMinutes()));
        model.put("cancellationDeadline", WorkshopDates.dateInSentence(cancellationDeadline) + " à " + WorkshopDates.time(cancellationDeadline));
        model.put("cancelUrl", publicBaseUrl + "/workshops/bookings/" + workshopBooking.getToken() + "/cancel");

        mailProvider.send(new Mail(
                workshopBooking.getEmail(),
                "Votre place est réservée — Atelier EOSA chez " + workshopVenue.getName(),
                "workshop-booking-confirmation",
                model
        ));
        workshopBooking.setConfirmationEmailSentAt(clock.instant());
        workshopBookingProvider.write(workshopBooking);
    }

    public record Command(
            WorkshopBookingId workshopBookingId
    ) {
    }

}
