package me.noynto.eosa.application;

import me.noynto.eosa.workshop.WorkshopWaitlistEntry;
import me.noynto.eosa.workshop.WorkshopWaitlistEntryProvider;

import java.time.Clock;

/**
 * Désinscription de la liste d'attente, depuis le lien des e-mails « Place libérée ».
 */
public record LeaveWorkshopWaitlist(
        WorkshopWaitlistEntryProvider workshopWaitlistEntryProvider,
        Clock clock
) {

    public WorkshopWaitlistEntry handle(Command command) {
        if (command.token == null || command.token.isBlank()) {
            throw new RuntimeException("Le lien est incomplet.");
        }
        WorkshopWaitlistEntry entry = workshopWaitlistEntryProvider.readIds(null, null, command.token(), null)
                .findFirst()
                .flatMap(workshopWaitlistEntryProvider::read)
                .orElseThrow(() -> new RuntimeException("Ce lien n'est pas valide."));
        if (entry.isWaiting()) {
            entry.setRemovedAt(clock.instant());
            workshopWaitlistEntryProvider.write(entry);
        }
        return entry;
    }

    public record Command(
            String token
    ) {
    }

}
