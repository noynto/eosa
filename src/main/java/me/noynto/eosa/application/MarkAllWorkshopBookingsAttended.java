package me.noynto.eosa.application;

import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.workshop.WorkshopBooking;
import me.noynto.eosa.workshop.WorkshopBookingProvider;
import me.noynto.eosa.workshop.WorkshopBookingState;
import me.noynto.eosa.workshop.WorkshopSession;
import me.noynto.eosa.workshop.WorkshopSessionProvider;
import me.noynto.eosa.workshop.WorkshopSessionState;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;

/**
 * Raccourci de fin d'atelier : marque présentes toutes les inscrites encore seulement « confirmées »,
 * sans toucher à celles déjà marquées présentes ou absentes.
 */
public record MarkAllWorkshopBookingsAttended(
        WorkshopBookingProvider workshopBookingProvider,
        WorkshopSessionProvider workshopSessionProvider,
        Clock clock
) {

    /**
     * @return le nombre de participantes marquées présentes
     */
    public int handle(Command command) {
        if (command.workshopSessionId == null || command.workshopSessionId.value() == null) {
            throw new RuntimeException("L'identifiant de la session est nécessaire.");
        }
        WorkshopSession workshopSession = workshopSessionProvider.read(command.workshopSessionId)
                .orElseThrow(() -> new RuntimeException("La session " + command.workshopSessionId.value() + " n'existe pas."));
        Instant now = clock.instant();
        if (workshopSession.getState() == WorkshopSessionState.CANCELLED) {
            throw new RuntimeException("Cette session a été annulée.");
        }
        if (now.isBefore(workshopSession.getStartsAt())) {
            throw new RuntimeException("La séance n'a pas encore commencé.");
        }

        int marked = 0;
        var confirmedIds = workshopBookingProvider
                .readIds(workshopSession.getId(), EnumSet.of(WorkshopBookingState.CONFIRMED), null, null)
                .toList();
        for (var id : confirmedIds) {
            WorkshopBooking workshopBooking = workshopBookingProvider.read(id).orElseThrow();
            workshopBooking.setState(WorkshopBookingState.ATTENDED);
            workshopBookingProvider.write(workshopBooking);
            marked++;
        }
        return marked;
    }

    public record Command(
            WorkshopSessionId workshopSessionId
    ) {
    }

}
