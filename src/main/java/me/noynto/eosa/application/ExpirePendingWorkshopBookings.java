package me.noynto.eosa.application;

import me.noynto.eosa.workshop.WorkshopBooking;
import me.noynto.eosa.workshop.WorkshopBookingProvider;
import me.noynto.eosa.workshop.WorkshopBookingState;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;

/**
 * Passe en EXPIRED les demandes de réservation non confirmées dans le délai. Les places sont déjà
 * libérées à la fin du délai (une demande expirée n'occupe plus de place) : cette tâche ne fait que
 * mettre l'état à jour.
 */
public record ExpirePendingWorkshopBookings(
        WorkshopBookingProvider workshopBookingProvider,
        Clock clock
) {

    /**
     * @return le nombre de réservations expirées
     */
    public int handle() {
        Instant now = clock.instant();
        int expired = 0;
        var pendingIds = workshopBookingProvider.readIds(null, EnumSet.of(WorkshopBookingState.PENDING), null, null).toList();
        for (var id : pendingIds) {
            WorkshopBooking workshopBooking = workshopBookingProvider.read(id).orElseThrow();
            if (workshopBooking.getState() == WorkshopBookingState.PENDING && !workshopBooking.isAwaitingConfirmation(now)) {
                workshopBooking.setState(WorkshopBookingState.EXPIRED);
                workshopBookingProvider.write(workshopBooking);
                expired++;
            }
        }
        return expired;
    }

}
