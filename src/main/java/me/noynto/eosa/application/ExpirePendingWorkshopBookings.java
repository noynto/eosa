package me.noynto.eosa.application;

import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.workshop.WorkshopBooking;
import me.noynto.eosa.workshop.WorkshopBookingProvider;
import me.noynto.eosa.workshop.WorkshopBookingState;

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
        NotifyWorkshopWaitlist notifyWorkshopWaitlist,
        Clock clock
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
                notifyWorkshopWaitlist.handle(new NotifyWorkshopWaitlist.Command(sessionId));
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING, "La liste d'attente de la session " + sessionId.value() + " n'a pas pu être prévenue.", e);
            }
        }
        return expired;
    }

}
