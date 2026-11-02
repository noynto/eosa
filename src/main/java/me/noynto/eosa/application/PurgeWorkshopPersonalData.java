package me.noynto.eosa.application;

import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.workshop.WorkshopBooking;
import me.noynto.eosa.workshop.WorkshopBookingProvider;
import me.noynto.eosa.workshop.WorkshopSession;
import me.noynto.eosa.workshop.WorkshopSessionProvider;
import me.noynto.eosa.workshop.WorkshopSessionState;
import me.noynto.eosa.workshop.WorkshopWaitlistEntry;
import me.noynto.eosa.workshop.WorkshopWaitlistEntryProvider;

import java.time.Clock;
import java.time.Instant;
import java.time.Period;
import java.util.HashMap;
import java.util.Map;

/**
 * Supprime les données personnelles des ateliers au terme des durées de conservation annoncées dans la
 * politique de confidentialité :
 * <ul>
 *     <li>les réservations (présence ou absence comprise), 1 an après la date de l'atelier ; le blocage de
 *     la réservation en ligne après une absence disparaît avec elles ;</li>
 *     <li>les inscriptions en liste d'attente, dès que la personne a quitté la liste (désinscription,
 *     place réservée, session annulée) ou que l'atelier a eu lieu.</li>
 * </ul>
 * Les sessions et les lieux, qui ne contiennent pas de données personnelles, sont gardés pour l'historique.
 */
public record PurgeWorkshopPersonalData(
        WorkshopSessionProvider workshopSessionProvider,
        WorkshopBookingProvider workshopBookingProvider,
        WorkshopWaitlistEntryProvider workshopWaitlistEntryProvider,
        Clock clock
) {

    public static final Period BOOKING_RETENTION = Period.ofYears(1);

    public Result handle() {
        Instant now = clock.instant();
        Map<WorkshopSessionId, WorkshopSession> sessions = new HashMap<>();

        int deletedBookings = 0;
        var bookingIds = workshopBookingProvider.readIds(null, null, null, null).toList();
        for (var id : bookingIds) {
            WorkshopBooking workshopBooking = workshopBookingProvider.read(id).orElseThrow();
            WorkshopSession workshopSession = sessions.computeIfAbsent(workshopBooking.getSessionId(),
                    sessionId -> workshopSessionProvider.read(sessionId).orElseThrow());
            Instant deleteAfter = workshopSession.getEndsAt().atZone(WorkshopSession.ZONE).plus(BOOKING_RETENTION).toInstant();
            if (!now.isBefore(deleteAfter)) {
                workshopBookingProvider.delete(id);
                deletedBookings++;
            }
        }

        int deletedWaitlistEntries = 0;
        var entryIds = workshopWaitlistEntryProvider.readIds(null, null, null, null).toList();
        for (var id : entryIds) {
            WorkshopWaitlistEntry entry = workshopWaitlistEntryProvider.read(id).orElseThrow();
            WorkshopSession workshopSession = sessions.computeIfAbsent(entry.getSessionId(),
                    sessionId -> workshopSessionProvider.read(sessionId).orElseThrow());
            boolean sessionOver = workshopSession.getState() == WorkshopSessionState.CANCELLED
                    || !now.isBefore(workshopSession.getEndsAt());
            if (!entry.isWaiting() || sessionOver) {
                workshopWaitlistEntryProvider.delete(id);
                deletedWaitlistEntries++;
            }
        }
        return new Result(deletedBookings, deletedWaitlistEntries);
    }

    public record Result(
            int deletedBookings,
            int deletedWaitlistEntries
    ) {
    }

}
