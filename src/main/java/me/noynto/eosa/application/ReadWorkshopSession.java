package me.noynto.eosa.application;

import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.workshop.WorkshopSessionProvider;

public record ReadWorkshopSession(
        WorkshopSessionProvider workshopSessionProvider,
        ReadWorkshopSessions readWorkshopSessions
) {

    public ReadWorkshopSessions.Overview handle(Command command) {
        if (command.workshopSessionId == null || command.workshopSessionId.value() == null) {
            throw new RuntimeException("L'identifiant de la session est nécessaire.");
        }
        return workshopSessionProvider.read(command.workshopSessionId())
                .map(readWorkshopSessions::overviewOf)
                .orElseThrow(() -> new RuntimeException("La session " + command.workshopSessionId.value() + " n'existe pas."));
    }

    public record Command(
            WorkshopSessionId workshopSessionId
    ) {
    }

}
