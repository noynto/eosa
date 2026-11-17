package me.noynto.eosa.application;

import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.shared.WorkshopVenueId;
import me.noynto.eosa.workshop.WorkshopBookingProvider;
import me.noynto.eosa.workshop.WorkshopSession;
import me.noynto.eosa.workshop.WorkshopSessionProvider;
import me.noynto.eosa.workshop.WorkshopSessionState;
import me.noynto.eosa.workshop.WorkshopVenue;
import me.noynto.eosa.workshop.WorkshopVenueProvider;
import me.noynto.eosa.workshop.WorkshopWaitlistEntryProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReadDashboardWorkshopsTest {

    @Mock WorkshopSessionProvider sessionProvider;
    @Mock WorkshopVenueProvider venueProvider;
    @Mock WorkshopBookingProvider bookingProvider;
    @Mock WorkshopWaitlistEntryProvider waitlistProvider;

    private final Instant now = Instant.parse("2026-11-18T11:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private final WorkshopVenueId venueId = new WorkshopVenueId("venue");

    @BeforeEach
    void setUp() {
        var venue = new WorkshopVenue();
        venue.setId(venueId);
        venue.setName("Bar Le Comptoir");
        when(venueProvider.read(venueId)).thenReturn(Optional.of(venue));
        when(bookingProvider.readIds(any(), any(), any(), any())).thenAnswer(inv -> Stream.empty());
        when(waitlistProvider.readIds(any(), any(), any(), any())).thenAnswer(inv -> Stream.empty());
    }

    private WorkshopSession session(String id, Instant startsAt, WorkshopSessionState state) {
        var session = new WorkshopSession();
        session.setId(new WorkshopSessionId(id));
        session.setVenueId(venueId);
        session.setStartsAt(startsAt);
        session.setDurationMinutes(180);
        session.setCapacity(6);
        session.setState(state);
        when(sessionProvider.read(session.getId())).thenReturn(Optional.of(session));
        return session;
    }

    @Test
    void handle_listsTheNextThreeScheduledSessionsStillToCome() {
        var past = session("past", now.minus(Duration.ofDays(2)), WorkshopSessionState.SCHEDULED);
        var cancelled = session("cancelled", now.plus(Duration.ofDays(1)), WorkshopSessionState.CANCELLED);
        var first = session("first", now.plus(Duration.ofDays(3)), WorkshopSessionState.SCHEDULED);
        var second = session("second", now.plus(Duration.ofDays(10)), WorkshopSessionState.SCHEDULED);
        var third = session("third", now.plus(Duration.ofDays(20)), WorkshopSessionState.SCHEDULED);
        var fourth = session("fourth", now.plus(Duration.ofDays(30)), WorkshopSessionState.SCHEDULED);
        when(sessionProvider.readIds()).thenAnswer(inv ->
                Stream.of(past, cancelled, first, second, third, fourth).map(WorkshopSession::getId));

        var overviews = new ReadDashboardWorkshops(sessionProvider, venueProvider, bookingProvider, waitlistProvider, clock)
                .handle().overviews();

        assertEquals(List.of("first", "second", "third"),
                overviews.stream().map(overview -> overview.session().getId().value()).toList());
        assertEquals("Bar Le Comptoir", overviews.getFirst().venue().getName());
        assertEquals(0, overviews.getFirst().occupiedPlaces());
    }

    @Test
    void handle_isEmptyWithoutUpcomingSession() {
        when(sessionProvider.readIds()).thenAnswer(inv -> Stream.empty());

        assertEquals(0, new ReadDashboardWorkshops(sessionProvider, venueProvider, bookingProvider, waitlistProvider, clock)
                .handle().overviews().size());
    }

}
