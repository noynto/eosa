package me.noynto.eosa.application;

import me.noynto.eosa.mail.MailProvider;
import me.noynto.eosa.service.WorkshopMails;
import me.noynto.eosa.service.WorkshopPlaces;
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
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Passe en EXPIRED les demandes de réservation non confirmées dans le délai, puis prévient la liste
 * d'attente des sessions concernées. Les places sont déjà libérées à la fin du délai (une demande
 * expirée n'occupe plus de place) : cette tâche met l'état à jour et fait savoir que la place est libre.
 */
public record ExpirePendingWorkshopBookings(
        WorkshopBookingProvider workshopBookingProvider,
        WorkshopSessionProvider workshopSessionProvider,
        WorkshopVenueProvider workshopVenueProvider,
        WorkshopWaitlistEntryProvider workshopWaitlistEntryProvider,
        MailProvider mailProvider,
        Clock clock,
        URI publicBaseUrl
) {

    private static final Logger LOGGER = Logger.getLogger(ExpirePendingWorkshopBookings.class.getName());

    /**
     * @return le nombre de réservations expirées
     */
    public int handle() {
        Instant now = clock.instant();
        Set<WorkshopSessionId> sessionsWithFreedPlaces = new LinkedHashSet<>();
        int expired = 0;
        var pendingIds = workshopBookingProvider.readIds(null, EnumSet.of(WorkshopBookingState.PENDING), null, null).toList();
        for (var id : pendingIds) {
            WorkshopBooking workshopBooking = workshopBookingProvider.read(id).orElseThrow();
            if (workshopBooking.getState() == WorkshopBookingState.PENDING && !workshopBooking.isAwaitingConfirmation(now)) {
                workshopBooking.setState(WorkshopBookingState.EXPIRED);
                workshopBookingProvider.write(workshopBooking);
                sessionsWithFreedPlaces.add(workshopBooking.getSessionId());
                expired++;
            }
        }
        for (var sessionId : sessionsWithFreedPlaces) {
            try {
                notifyWaitlist(sessionId, now);
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING, "La liste d'attente de la session " + sessionId.value() + " n'a pas pu être prévenue.", e);
            }
        }
        return expired;
    }

    /**
     * La place libérée est annoncée à toute la liste d'attente : la première personne qui réserve l'obtient.
     */
    private void notifyWaitlist(WorkshopSessionId sessionId, Instant now) {
        WorkshopSession workshopSession = workshopSessionProvider.read(sessionId).orElseThrow();
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
        WorkshopVenue workshopVenue = workshopVenueProvider.read(workshopSession.getVenueId()).orElseThrow();
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

}
