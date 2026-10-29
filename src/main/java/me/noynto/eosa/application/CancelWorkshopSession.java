package me.noynto.eosa.application;

import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.workshop.WorkshopSession;
import me.noynto.eosa.workshop.WorkshopSessionProvider;
import me.noynto.eosa.workshop.WorkshopSessionState;

import java.time.Clock;
import java.time.Instant;

public record CancelWorkshopSession(
        WorkshopSessionProvider workshopSessionProvider,
        Clock clock
) {

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
        return workshopSessionProvider.write(workshopSession);
    }

    public record Command(
            WorkshopSessionId workshopSessionId
    ) {
    }

}
