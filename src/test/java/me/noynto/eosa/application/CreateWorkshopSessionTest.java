package me.noynto.eosa.application;

import me.noynto.eosa.shared.WorkshopVenueId;
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
class CreateWorkshopSessionTest {

    @Mock WorkshopVenueProvider workshopVenueProvider;
    @Mock WorkshopSessionProvider workshopSessionProvider;

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-01T10:00:00Z"), ZoneOffset.UTC);
    private final WorkshopVenueId venueId = new WorkshopVenueId("venue");
    private final LocalDateTime nextSaturday = LocalDateTime.of(2026, 11, 14, 14, 0);

    @Test
    void handle_createsScheduledSessionInParisTime() {
        when(workshopVenueProvider.read(venueId)).thenReturn(Optional.of(venue(false)));
        when(workshopSessionProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        WorkshopSession result = createWorkshopSession().handle(
                new CreateWorkshopSession.Command(venueId, nextSaturday, 90, 6)
        );

        assertEquals(venueId, result.getVenueId());
        assertEquals(Instant.parse("2026-11-14T13:00:00Z"), result.getStartsAt());
        assertEquals(90, result.getDurationMinutes());
        assertEquals(6, result.getCapacity());
        assertEquals(WorkshopSessionState.SCHEDULED, result.getState());
    }

    @Test
    void handle_usesVenueDefaultsWhenCapacityAndDurationAreMissing() {
        when(workshopVenueProvider.read(venueId)).thenReturn(Optional.of(venue(false)));
        when(workshopSessionProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        WorkshopSession result = createWorkshopSession().handle(
                new CreateWorkshopSession.Command(venueId, nextSaturday, null, null)
        );

        assertEquals(10, result.getCapacity());
        assertEquals(150, result.getDurationMinutes());
    }

    @Test
    void handle_throwsWhenVenueIsArchived() {
        when(workshopVenueProvider.read(venueId)).thenReturn(Optional.of(venue(true)));

        assertThrows(RuntimeException.class, () -> createWorkshopSession().handle(
                new CreateWorkshopSession.Command(venueId, nextSaturday, null, null)
        ));
        verify(workshopSessionProvider, never()).write(any());
    }

    @Test
    void handle_throwsWhenSessionStartsInThePast() {
        when(workshopVenueProvider.read(venueId)).thenReturn(Optional.of(venue(false)));

        assertThrows(RuntimeException.class, () -> createWorkshopSession().handle(
                new CreateWorkshopSession.Command(venueId, LocalDateTime.of(2026, 10, 31, 14, 0), null, null)
        ));
        verify(workshopSessionProvider, never()).write(any());
    }

    @Test
    void handle_throwsWhenCapacityIsOutOfBounds() {
        when(workshopVenueProvider.read(venueId)).thenReturn(Optional.of(venue(false)));

        assertThrows(RuntimeException.class, () -> createWorkshopSession().handle(
                new CreateWorkshopSession.Command(venueId, nextSaturday, 120, 0)
        ));
    }

    @Test
    void handle_throwsWhenStartIsMissing() {
        assertThrows(RuntimeException.class, () -> createWorkshopSession().handle(
                new CreateWorkshopSession.Command(venueId, null, null, null)
        ));
    }

    @Test
    void handle_throwsWhenVenueNotFound() {
        when(workshopVenueProvider.read(venueId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> createWorkshopSession().handle(
                new CreateWorkshopSession.Command(venueId, nextSaturday, null, null)
        ));
    }

    private CreateWorkshopSession createWorkshopSession() {
        return new CreateWorkshopSession(workshopVenueProvider, workshopSessionProvider, clock);
    }

    private WorkshopVenue venue(boolean archived) {
        var venue = new WorkshopVenue();
        venue.setId(venueId);
        venue.setName("Café des Arts");
        venue.setDefaultCapacity(10);
        venue.setDefaultDurationMinutes(150);
        venue.setArchived(archived);
        return venue;
    }

}
