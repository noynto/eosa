package me.noynto.eosa.application;

import me.noynto.eosa.jewel.JewelProvider;
import me.noynto.eosa.jewel.JewelState;
import me.noynto.eosa.shared.JewelId;
import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.workshop.WorkshopSession;
import me.noynto.eosa.workshop.WorkshopSessionProvider;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Ce que les moteurs de recherche doivent connaître : les bijoux publiés et les sessions d'atelier pas encore terminées
 * (une session annulée reste listée jusqu'à sa date, pour que Google affiche l'annulation).
 */
public record ReadSitemap(
        JewelProvider jewelProvider,
        WorkshopSessionProvider workshopSessionProvider,
        Clock clock
) {

    public Result handle() {
        Instant now = clock.instant();
        List<JewelId> jewelIds = jewelProvider.readIds(Set.of(JewelState.PUBLISHED), Set.of()).toList();
        List<WorkshopSessionId> workshopSessionIds = workshopSessionProvider.readIds()
                .map(id -> workshopSessionProvider.read(id).orElseThrow())
                .filter(workshopSession -> workshopSession.getEndsAt().isAfter(now))
                .map(WorkshopSession::getId)
                .toList();
        return new Result(jewelIds, workshopSessionIds);
    }

    public record Result(
            List<JewelId> jewelIds,
            List<WorkshopSessionId> workshopSessionIds
    ) {
    }

}
