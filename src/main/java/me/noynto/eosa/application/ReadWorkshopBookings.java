package me.noynto.eosa.application;

import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.workshop.WorkshopBooking;
import me.noynto.eosa.workshop.WorkshopBookingProvider;
import me.noynto.eosa.workshop.WorkshopBookingState;
import me.noynto.eosa.workshop.WorkshopSession;
import me.noynto.eosa.workshop.WorkshopSessionProvider;

import java.util.EnumSet;
import java.util.List;

/**
 * Inscrites d'une session pour l'administration : toutes les réservations sauf les demandes jamais confirmées,
 * avec la session, dont dépend ce qu'on peut faire de chacune (annuler, pointer).
 */
public record ReadWorkshopBookings(
        WorkshopBookingProvider workshopBookingProvider,
        WorkshopSessionProvider workshopSessionProvider
) {

    public Result handle(Command command) {
        if (command.workshopSessionId == null || command.workshopSessionId.value() == null) {
            throw new RuntimeException("L'identifiant de la session est nécessaire.");
        }
        WorkshopSession workshopSession = workshopSessionProvider.read(command.workshopSessionId())
                .orElseThrow(() -> new RuntimeException("La session " + command.workshopSessionId.value() + " n'existe pas."));
        List<WorkshopBooking> bookings = workshopBookingProvider
                .readIds(workshopSession.getId(), EnumSet.complementOf(EnumSet.of(WorkshopBookingState.EXPIRED)), null, null)
                .map(id -> workshopBookingProvider.read(id).orElseThrow())
                .toList();
        return new Result(workshopSession, bookings);
    }

    public record Command(
            WorkshopSessionId workshopSessionId
    ) {
    }

    public record Result(
            WorkshopSession session,
            List<WorkshopBooking> bookings
    ) {
    }

}
