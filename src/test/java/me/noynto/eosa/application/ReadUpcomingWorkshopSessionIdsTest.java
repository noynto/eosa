package me.noynto.eosa.application;

import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.workshop.WorkshopSession;
import me.noynto.eosa.workshop.WorkshopSessionProvider;
import me.noynto.eosa.workshop.WorkshopSessionState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadUpcomingWorkshopSessionIdsTest {

    final Instant now = Instant.parse("2026-11-20T10:00:00Z");
    final Clock clock = Clock.fixed(now, ZoneOffset.UTC);

    @Mock WorkshopSessionProvider workshopSessionProvider;

    @Test
    void handle_keepsScheduledSessionsThatHaveNotStartedInTheirOrder() {
        var started = session("started", now.minusSeconds(60), WorkshopSessionState.SCHEDULED);
        var cancelled = session("cancelled", now.plusSeconds(3600), WorkshopSessionState.CANCELLED);
        var first = session("first", now.plusSeconds(7200), WorkshopSessionState.SCHEDULED);
        var second = session("second", now.plusSeconds(86400), WorkshopSessionState.SCHEDULED);
        sessionsAre(started, cancelled, first, second);

        var ids = handle(5);

        assertEquals(List.of(new WorkshopSessionId("first"), new WorkshopSessionId("second")), ids);
    }

    @Test
    void handle_stopsAtTheLimit() {
        sessionsAre(
                session("a", now.plusSeconds(60), WorkshopSessionState.SCHEDULED),
                session("b", now.plusSeconds(120), WorkshopSessionState.SCHEDULED),
                session("c", now.plusSeconds(180), WorkshopSessionState.SCHEDULED)
        );

        assertEquals(List.of(new WorkshopSessionId("a"), new WorkshopSessionId("b")), handle(2));
    }

    @Test
    void handle_returnsNothingWhenNoSessionIsUpcoming() {
        sessionsAre(session("past", now.minusSeconds(60), WorkshopSessionState.SCHEDULED));

        assertEquals(List.of(), handle(3));
    }

    private List<WorkshopSessionId> handle(int limit) {
        return new ReadUpcomingWorkshopSessionIds(workshopSessionProvider, clock)
                .handle(new ReadUpcomingWorkshopSessionIds.Query(limit))
                .workshopSessionIds();
    }

    private void sessionsAre(WorkshopSession... sessions) {
        when(workshopSessionProvider.readIds()).thenReturn(Stream.of(sessions).map(WorkshopSession::getId));
        // La lecture s'arrête à la limite : les dernières sessions ne sont pas toujours lues.
        for (WorkshopSession session : sessions) {
            lenient().when(workshopSessionProvider.read(session.getId())).thenReturn(Optional.of(session));
        }
    }

    private static WorkshopSession session(String id, Instant startsAt, WorkshopSessionState state) {
        var session = new WorkshopSession();
        session.setId(new WorkshopSessionId(id));
        session.setStartsAt(startsAt);
        session.setState(state);
        return session;
    }

}
