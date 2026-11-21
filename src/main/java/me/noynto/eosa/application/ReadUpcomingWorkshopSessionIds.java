package me.noynto.eosa.application;

import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.workshop.WorkshopSessionProvider;
import me.noynto.eosa.workshop.WorkshopSessionState;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Les prochaines sessions programmées, par date de début croissante : celles qui n'ont pas commencé et ne sont pas
 * annulées. Chaque carte se charge ensuite d'elle-même.
 */
public record ReadUpcomingWorkshopSessionIds(
        WorkshopSessionProvider workshopSessionProvider,
        Clock clock
) {

    public Result handle(Query query) {
        Instant now = clock.instant();
        List<WorkshopSessionId> ids = workshopSessionProvider.readIds()
                .map(id -> workshopSessionProvider.read(id).orElseThrow())
                .filter(session -> session.getState() == WorkshopSessionState.SCHEDULED)
                .filter(session -> session.getStartsAt().isAfter(now))
                .limit(query.limit())
                .map(session -> session.getId())
                .toList();
        return new Result(ids);
    }

    public record Result(
            List<WorkshopSessionId> workshopSessionIds
    ) {
    }

    public record Query(
            int limit
    ) {
    }

}
