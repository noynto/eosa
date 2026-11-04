package me.noynto.eosa.application;

import me.noynto.eosa.mail.MailProvider;
import me.noynto.eosa.service.WorkshopMails;
import me.noynto.eosa.service.WorkshopPlaces;
import me.noynto.eosa.shared.WorkshopBookingId;
import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.workshop.WorkshopBooking;
import me.noynto.eosa.workshop.WorkshopBookingProvider;
import me.noynto.eosa.workshop.WorkshopBookingState;
import me.noynto.eosa.workshop.WorkshopSession;
import me.noynto.eosa.workshop.WorkshopSessionProvider;
import me.noynto.eosa.workshop.WorkshopVenue;
import me.noynto.eosa.workshop.WorkshopVenueProvider;
import me.noynto.eosa.workshop.WorkshopWaitlistEntry;
import me.noynto.eosa.workshop.WorkshopWaitlistEntryProvider;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Annulation d'une réservation par l'administration, sans contrainte de délai (motif valable reçu sur Instagram…).
 */
public record CancelWorkshopBookingByAdmin(
        WorkshopBookingProvider workshopBookingProvider,
        WorkshopSessionProvider workshopSessionProvider,
        WorkshopVenueProvider workshopVenueProvider,
        WorkshopWaitlistEntryProvider workshopWaitlistEntryProvider,
        MailProvider mailProvider,
        Clock clock,
        URI publicBaseUrl
) {

    private static final Logger LOGGER = Logger.getLogger(CancelWorkshopBookingByAdmin.class.getName());

    public WorkshopBooking handle(Command command) {
        if (command.workshopSessionId == null || command.workshopBookingId == null || command.workshopBookingId.value() == null) {
            throw new RuntimeException("La session et la réservation sont nécessaires.");
        }
        WorkshopBooking workshopBooking = workshopBookingProvider.read(command.workshopBookingId)
                .filter(booking -> booking.getSessionId().equals(command.workshopSessionId()))
                .orElseThrow(() -> new RuntimeException("Cette réservation n'existe pas sur cette session."));
        Instant now = clock.instant();
        if (!workshopBooking.isActive(now)) {
            throw new RuntimeException("Cette réservation n'est plus active.");
        }

        workshopBooking.setState(WorkshopBookingState.CANCELLED);
        workshopBooking.setCancelledAt(now);
        workshopBookingProvider.write(workshopBooking);

        WorkshopSession workshopSession = workshopSessionProvider.read(workshopBooking.getSessionId()).orElseThrow();
        WorkshopVenue workshopVenue = workshopVenueProvider.read(workshopSession.getVenueId()).orElseThrow();
        // L'annulation est faite même si l'e-mail ne part pas : il sera renvoyé plus tard.
        try {
            mailProvider.send(WorkshopMails.bookingCancellation(workshopBooking, workshopSession, workshopVenue, publicBaseUrl));
            workshopBooking.setCancellationEmailSentAt(now);
            workshopBookingProvider.write(workshopBooking);
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "L'e-mail d'annulation de la réservation " + workshopBooking.getId().value()
                    + " n'est pas parti, il sera renvoyé plus tard.", e);
        }
        try {
            notifyWaitlist(workshopSession, workshopVenue, now);
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "La liste d'attente de la session " + workshopSession.getId().value()
                    + " n'a pas pu être prévenue.", e);
        }
        return workshopBooking;
    }

    /**
     * La place libérée est annoncée à toute la liste d'attente : la première personne qui réserve l'obtient.
     */
    private void notifyWaitlist(WorkshopSession workshopSession, WorkshopVenue workshopVenue, Instant now) {
        if (!workshopSession.isBookable(now)) {
            return;
        }
        var bookings = workshopBookingProvider
                .readIds(workshopSession.getId(), WorkshopPlaces.OCCUPYING_STATES, null, null)
                .map(id -> workshopBookingProvider.read(id).orElseThrow())
                .toList();
        if (WorkshopPlaces.free(workshopSession, bookings, now) == 0) {
            return;
        }
        for (var entryId : workshopWaitlistEntryProvider.readIds(workshopSession.getId(), null, null, true).toList()) {
            WorkshopWaitlistEntry entry = workshopWaitlistEntryProvider.read(entryId).orElseThrow();
            try {
                mailProvider.send(WorkshopMails.waitlistPlaceAvailable(entry, workshopSession, workshopVenue, publicBaseUrl));
                entry.setLastNotifiedAt(now);
                workshopWaitlistEntryProvider.write(entry);
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING, "L'e-mail « Place libérée » n'est pas parti pour l'inscription " + entryId.value() + ".", e);
            }
        }
    }

    public record Command(
            WorkshopSessionId workshopSessionId,
            WorkshopBookingId workshopBookingId
    ) {
    }

}
