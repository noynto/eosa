package me.noynto.eosa.application;

import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.workshop.WorkshopWaitlistEntry;
import me.noynto.eosa.workshop.WorkshopWaitlistEntryProvider;

import java.util.List;

/**
 * Personnes encore en attente sur une session, par ordre d'inscription.
 */
public record ReadWorkshopWaitlist(
        WorkshopWaitlistEntryProvider workshopWaitlistEntryProvider
) {

    public List<WorkshopWaitlistEntry> handle(Command command) {
        if (command.workshopSessionId == null || command.workshopSessionId.value() == null) {
            throw new RuntimeException("L'identifiant de la session est nécessaire.");
        }
        return workshopWaitlistEntryProvider.readIds(command.workshopSessionId(), null, null, true)
                .map(id -> workshopWaitlistEntryProvider.read(id).orElseThrow())
                .toList();
    }

    public record Command(
            WorkshopSessionId workshopSessionId
    ) {
    }

}
