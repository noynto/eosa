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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CancelWorkshopSessionTest {

    @Mock WorkshopSessionProvider workshopSessionProvider;

    private final Instant now = Instant.parse("2026-11-01T10:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private final WorkshopSessionId sessionId = new WorkshopSessionId("session");

    @Test
    void handle_cancelsUpcomingSession() {
        when(workshopSessionProvider.read(sessionId)).thenReturn(Optional.of(sessionStartingAt("2026-11-14T13:00:00Z")));
        when(workshopSessionProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        WorkshopSession result = new CancelWorkshopSession(workshopSessionProvider, clock).handle(
                new CancelWorkshopSession.Command(sessionId)
        );

        assertEquals(WorkshopSessionState.CANCELLED, result.getState());
        assertEquals(now, result.getCancelledAt());
    }

    @Test
    void handle_refusesAlreadyCancelledSession() {
        var session = sessionStartingAt("2026-11-14T13:00:00Z");
        session.setState(WorkshopSessionState.CANCELLED);
        when(workshopSessionProvider.read(sessionId)).thenReturn(Optional.of(session));

        assertThrows(RuntimeException.class, () -> new CancelWorkshopSession(workshopSessionProvider, clock).handle(
                new CancelWorkshopSession.Command(sessionId)
        ));
        verify(workshopSessionProvider, never()).write(any());
    }

    @Test
    void handle_refusesStartedSession() {
        when(workshopSessionProvider.read(sessionId)).thenReturn(Optional.of(sessionStartingAt("2026-11-01T09:30:00Z")));

        assertThrows(RuntimeException.class, () -> new CancelWorkshopSession(workshopSessionProvider, clock).handle(
                new CancelWorkshopSession.Command(sessionId)
        ));
        verify(workshopSessionProvider, never()).write(any());
    }

    @Test
    void handle_throwsWhenSessionNotFound() {
        when(workshopSessionProvider.read(sessionId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> new CancelWorkshopSession(workshopSessionProvider, clock).handle(
                new CancelWorkshopSession.Command(sessionId)
        ));
    }

    private WorkshopSession sessionStartingAt(String instant) {
        var session = new WorkshopSession();
        session.setId(sessionId);
        session.setStartsAt(Instant.parse(instant));
        session.setDurationMinutes(120);
        session.setState(WorkshopSessionState.SCHEDULED);
        return session;
    }

}
