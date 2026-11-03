package me.noynto.eosa.application;

import me.noynto.eosa.jewel.JewelProvider;
import me.noynto.eosa.jewel.JewelState;
import me.noynto.eosa.shared.JewelId;
import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.workshop.WorkshopSession;
import me.noynto.eosa.workshop.WorkshopSessionProvider;
import me.noynto.eosa.workshop.WorkshopSessionState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadSitemapTest {

    @Mock JewelProvider jewelProvider;
    @Mock WorkshopSessionProvider workshopSessionProvider;

    private final Instant now = Instant.parse("2026-11-03T10:00:00Z");

    @Test
    void handle_listsPublishedJewelsAndSessionsNotEndedYet() {
        when(jewelProvider.readIds(Set.of(JewelState.PUBLISHED), Set.of()))
                .thenReturn(Stream.of(new JewelId("j1"), new JewelId("j2")));
        var ended = session("ended", now.minus(Duration.ofHours(3)), WorkshopSessionState.SCHEDULED);
        var inProgress = session("in-progress", now.minus(Duration.ofMinutes(30)), WorkshopSessionState.SCHEDULED);
        var cancelled = session("cancelled", now.plus(Duration.ofDays(2)), WorkshopSessionState.CANCELLED);
        when(workshopSessionProvider.readIds()).thenReturn(Stream.of(ended.getId(), inProgress.getId(), cancelled.getId()));
        for (var session : List.of(ended, inProgress, cancelled)) {
            when(workshopSessionProvider.read(session.getId())).thenReturn(Optional.of(session));
        }

        var result = new ReadSitemap(jewelProvider, workshopSessionProvider, Clock.fixed(now, ZoneOffset.UTC)).handle();

        assertEquals(List.of(new JewelId("j1"), new JewelId("j2")), result.jewelIds());
        assertEquals(List.of(inProgress.getId(), cancelled.getId()), result.workshopSessionIds());
    }

    private static WorkshopSession session(String id, Instant startsAt, WorkshopSessionState state) {
        var session = new WorkshopSession();
        session.setId(new WorkshopSessionId(id));
        session.setStartsAt(startsAt);
        session.setDurationMinutes(120);
        session.setState(state);
        return session;
    }

}
