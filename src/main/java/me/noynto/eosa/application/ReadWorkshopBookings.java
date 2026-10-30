package me.noynto.eosa.application;

import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.workshop.WorkshopBooking;
import me.noynto.eosa.workshop.WorkshopBookingProvider;
import me.noynto.eosa.workshop.WorkshopBookingState;

import java.util.EnumSet;
import java.util.List;

/**
 * Inscrites d'une session pour l'administration : toutes les réservations sauf les demandes jamais confirmées.
 */
public record ReadWorkshopBookings(
        WorkshopBookingProvider workshopBookingProvider
) {

    public List<WorkshopBooking> handle(Command command) {
        if (command.workshopSessionId == null || command.workshopSessionId.value() == null) {
            throw new RuntimeException("L'identifiant de la session est nécessaire.");
        }
        return workshopBookingProvider
                .readIds(command.workshopSessionId(), EnumSet.complementOf(EnumSet.of(WorkshopBookingState.EXPIRED)), null, null)
                .map(id -> workshopBookingProvider.read(id).orElseThrow())
                .toList();
    }

    public record Command(
            WorkshopSessionId workshopSessionId
    ) {
    }

}
