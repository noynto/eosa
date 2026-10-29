package me.noynto.eosa.application;

import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.shared.WorkshopVenueId;
import me.noynto.eosa.workshop.WorkshopBookingProvider;
import me.noynto.eosa.workshop.WorkshopSession;
import me.noynto.eosa.workshop.WorkshopSessionProvider;
import me.noynto.eosa.workshop.WorkshopSessionState;
import me.noynto.eosa.workshop.WorkshopVenue;
import me.noynto.eosa.workshop.WorkshopVenueProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateWorkshopSessionTest {

    @Mock WorkshopVenueProvider workshopVenueProvider;
    @Mock WorkshopSessionProvider workshopSessionProvider;
    @Mock WorkshopBookingProvider workshopBookingProvider;

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-01T10:00:00Z"), ZoneOffset.UTC);
    private final WorkshopSessionId sessionId = new WorkshopSessionId("session");
    private final WorkshopVenueId venueId = new WorkshopVenueId("venue");
    private final WorkshopVenueId otherVenueId = new WorkshopVenueId("other-venue");
    // 14 novembre 2026, 14 h 00 à Paris.
    private final LocalDateTime startsAt = LocalDateTime.of(2026, 11, 14, 14, 0);

    @Test
    void handle_updatesEverythingWhenNoPlaceIsBooked() {
        when(workshopSessionProvider.read(sessionId)).thenReturn(Optional.of(scheduledSession()));
        when(workshopVenueProvider.read(otherVenueId)).thenReturn(Optional.of(venue(otherVenueId, false)));
        when(workshopBookingProvider.countOccupiedPlaces(sessionId)).thenReturn(0);
        when(workshopSessionProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        WorkshopSession result = updateWorkshopSession().handle(new UpdateWorkshopSession.Command(
                sessionId, otherVenueId, LocalDateTime.of(2026, 11, 21, 10, 30), 90, 12
        ));

        assertEquals(otherVenueId, result.getVenueId());
        assertEquals(Instant.parse("2026-11-21T09:30:00Z"), result.getStartsAt());
        assertEquals(90, result.getDurationMinutes());
        assertEquals(12, result.getCapacity());
    }

    @Test
    void handle_allowsDurationAndCapacityChangesWhenPlacesAreBooked() {
        when(workshopSessionProvider.read(sessionId)).thenReturn(Optional.of(scheduledSession()));
        when(workshopBookingProvider.countOccupiedPlaces(sessionId)).thenReturn(3);
        when(workshopSessionProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        WorkshopSession result = updateWorkshopSession().handle(new UpdateWorkshopSession.Command(
                sessionId, venueId, startsAt, 150, 3
        ));

        assertEquals(150, result.getDurationMinutes());
        assertEquals(3, result.getCapacity());
    }

    @Test
    void handle_refusesDateChangeWhenPlacesAreBooked() {
        when(workshopSessionProvider.read(sessionId)).thenReturn(Optional.of(scheduledSession()));
        when(workshopBookingProvider.countOccupiedPlaces(sessionId)).thenReturn(1);

        assertThrows(RuntimeException.class, () -> updateWorkshopSession().handle(new UpdateWorkshopSession.Command(
                sessionId, venueId, startsAt.plusDays(1), 120, 8
        )));
        verify(workshopSessionProvider, never()).write(any());
    }

    @Test
    void handle_refusesVenueChangeWhenPlacesAreBooked() {
        when(workshopSessionProvider.read(sessionId)).thenReturn(Optional.of(scheduledSession()));
        when(workshopVenueProvider.read(otherVenueId)).thenReturn(Optional.of(venue(otherVenueId, false)));
        when(workshopBookingProvider.countOccupiedPlaces(sessionId)).thenReturn(1);

        assertThrows(RuntimeException.class, () -> updateWorkshopSession().handle(new UpdateWorkshopSession.Command(
                sessionId, otherVenueId, startsAt, 120, 8
        )));
        verify(workshopSessionProvider, never()).write(any());
    }

    @Test
    void handle_refusesCapacityBelowBookedPlaces() {
        when(workshopSessionProvider.read(sessionId)).thenReturn(Optional.of(scheduledSession()));
        when(workshopBookingProvider.countOccupiedPlaces(sessionId)).thenReturn(5);

        assertThrows(RuntimeException.class, () -> updateWorkshopSession().handle(new UpdateWorkshopSession.Command(
                sessionId, venueId, startsAt, 120, 4
        )));
        verify(workshopSessionProvider, never()).write(any());
    }

    @Test
    void handle_refusesArchivedVenue() {
        when(workshopSessionProvider.read(sessionId)).thenReturn(Optional.of(scheduledSession()));
        when(workshopVenueProvider.read(otherVenueId)).thenReturn(Optional.of(venue(otherVenueId, true)));

        assertThrows(RuntimeException.class, () -> updateWorkshopSession().handle(new UpdateWorkshopSession.Command(
                sessionId, otherVenueId, startsAt, 120, 8
        )));
    }

    @Test
    void handle_refusesNewStartInThePast() {
        when(workshopSessionProvider.read(sessionId)).thenReturn(Optional.of(scheduledSession()));

        assertThrows(RuntimeException.class, () -> updateWorkshopSession().handle(new UpdateWorkshopSession.Command(
                sessionId, venueId, LocalDateTime.of(2026, 10, 30, 14, 0), 120, 8
        )));
    }

    @Test
    void handle_refusesCancelledSession() {
        var session = scheduledSession();
        session.setState(WorkshopSessionState.CANCELLED);
        when(workshopSessionProvider.read(sessionId)).thenReturn(Optional.of(session));

        assertThrows(RuntimeException.class, () -> updateWorkshopSession().handle(new UpdateWorkshopSession.Command(
                sessionId, venueId, startsAt, 120, 8
        )));
    }

    @Test
    void handle_refusesStartedSession() {
        var session = scheduledSession();
        session.setStartsAt(Instant.parse("2026-11-01T09:00:00Z"));
        when(workshopSessionProvider.read(sessionId)).thenReturn(Optional.of(session));

        assertThrows(RuntimeException.class, () -> updateWorkshopSession().handle(new UpdateWorkshopSession.Command(
                sessionId, venueId, startsAt, 120, 8
        )));
    }

    private UpdateWorkshopSession updateWorkshopSession() {
        return new UpdateWorkshopSession(workshopVenueProvider, workshopSessionProvider, workshopBookingProvider, clock);
    }

    private WorkshopSession scheduledSession() {
        var session = new WorkshopSession();
        session.setId(sessionId);
        session.setVenueId(venueId);
        session.setStartsAt(Instant.parse("2026-11-14T13:00:00Z"));
        session.setDurationMinutes(120);
        session.setCapacity(8);
        session.setState(WorkshopSessionState.SCHEDULED);
        return session;
    }

    private WorkshopVenue venue(WorkshopVenueId id, boolean archived) {
        var venue = new WorkshopVenue();
        venue.setId(id);
        venue.setName("Bar du Coin");
        venue.setArchived(archived);
        return venue;
    }

}
