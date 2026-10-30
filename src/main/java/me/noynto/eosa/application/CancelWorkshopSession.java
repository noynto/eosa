package me.noynto.eosa.application;

import me.noynto.eosa.mail.Mail;
import me.noynto.eosa.mail.MailProvider;
import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.workshop.WorkshopBooking;
import me.noynto.eosa.workshop.WorkshopBookingProvider;
import me.noynto.eosa.workshop.WorkshopBookingState;
import me.noynto.eosa.workshop.WorkshopDates;
import me.noynto.eosa.workshop.WorkshopSession;
import me.noynto.eosa.workshop.WorkshopSessionProvider;
import me.noynto.eosa.workshop.WorkshopSessionState;
import me.noynto.eosa.workshop.WorkshopVenue;
import me.noynto.eosa.workshop.WorkshopVenueProvider;
import me.noynto.eosa.workshop.WorkshopWaitlistEntry;
import me.noynto.eosa.workshop.WorkshopWaitlistEntryProvider;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Annulation d'une session : ses réservations encore actives sont annulées et chaque inscrite est prévenue,
 * ainsi que les personnes en liste d'attente.
 */
public record CancelWorkshopSession(
        WorkshopSessionProvider workshopSessionProvider,
        WorkshopVenueProvider workshopVenueProvider,
        WorkshopBookingProvider workshopBookingProvider,
        WorkshopWaitlistEntryProvider workshopWaitlistEntryProvider,
        SendWorkshopBookingCancellation sendWorkshopBookingCancellation,
        MailProvider mailProvider,
        Clock clock,
        URI publicBaseUrl
) {

    private static final Logger LOGGER = Logger.getLogger(CancelWorkshopSession.class.getName());

    public WorkshopSession handle(Command command) {
        if (command.workshopSessionId == null || command.workshopSessionId.value() == null) {
            throw new RuntimeException("L'identifiant de la session est nécessaire.");
        }

        WorkshopSession workshopSession = workshopSessionProvider.read(command.workshopSessionId)
                .orElseThrow(() -> new RuntimeException("La session " + command.workshopSessionId.value() + " n'existe pas."));
        if (workshopSession.getState() == WorkshopSessionState.CANCELLED) {
            throw new RuntimeException("La session est déjà annulée.");
        }
        Instant now = clock.instant();
        if (!workshopSession.getStartsAt().isAfter(now)) {
            throw new RuntimeException("Une session commencée ne peut plus être annulée.");
        }

        workshopSession.setState(WorkshopSessionState.CANCELLED);
        workshopSession.setCancelledAt(now);
        workshopSessionProvider.write(workshopSession);

        var activeBookingIds = workshopBookingProvider
                .readIds(workshopSession.getId(), EnumSet.of(WorkshopBookingState.PENDING, WorkshopBookingState.CONFIRMED), null, null)
                .toList();
        for (var bookingId : activeBookingIds) {
            WorkshopBooking workshopBooking = workshopBookingProvider.read(bookingId).orElseThrow();
            if (!workshopBooking.isActive(now)) {
                continue;
            }
            workshopBooking.setState(WorkshopBookingState.CANCELLED);
            workshopBooking.setCancelledAt(now);
            workshopBookingProvider.write(workshopBooking);
            // Un e-mail qui ne part pas sera renvoyé plus tard : il ne doit pas bloquer les autres.
            try {
                sendWorkshopBookingCancellation.handle(new SendWorkshopBookingCancellation.Command(bookingId));
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING, "L'e-mail d'annulation de la réservation " + bookingId.value()
                        + " n'est pas parti, il sera renvoyé plus tard.", e);
            }
        }

        // La liste d'attente est fermée, et chaque personne en attente est prévenue (sans nouvelle tentative).
        WorkshopVenue workshopVenue = workshopVenueProvider.read(workshopSession.getVenueId()).orElseThrow();
        var waitingIds = workshopWaitlistEntryProvider.readIds(workshopSession.getId(), null, null, true).toList();
        for (var entryId : waitingIds) {
            WorkshopWaitlistEntry entry = workshopWaitlistEntryProvider.read(entryId).orElseThrow();
            entry.setRemovedAt(now);
            workshopWaitlistEntryProvider.write(entry);
            Map<String, Object> model = new HashMap<>();
            model.put("preheader", "L'atelier du " + WorkshopDates.dateInSentence(workshopSession.getStartsAt())
                    + " chez " + workshopVenue.getName() + " est annulé.");
            model.put("firstName", entry.getFirstName());
            model.put("sessionCancelled", true);
            model.put("waitlisted", true);
            model.put("venueName", workshopVenue.getName());
            model.put("date", WorkshopDates.date(workshopSession.getStartsAt()));
            model.put("timeRange", WorkshopDates.timeRange(workshopSession));
            model.put("workshopsUrl", publicBaseUrl + "/workshops");
            try {
                mailProvider.send(new Mail(
                        entry.getEmail(),
                        "Atelier annulé — Atelier EOSA chez " + workshopVenue.getName(),
                        "workshop-booking-cancellation",
                        model
                ));
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING, "L'e-mail d'annulation n'est pas parti pour l'inscription en liste d'attente "
                        + entryId.value() + ".", e);
            }
        }
        return workshopSession;
    }

    public record Command(
            WorkshopSessionId workshopSessionId
    ) {
    }

}
