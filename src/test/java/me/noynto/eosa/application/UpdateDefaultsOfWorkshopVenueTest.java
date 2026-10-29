package me.noynto.eosa.application;

import me.noynto.eosa.shared.WorkshopVenueId;
import me.noynto.eosa.workshop.WorkshopVenue;
import me.noynto.eosa.workshop.WorkshopVenueProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateDefaultsOfWorkshopVenueTest {

    @Mock WorkshopVenueProvider workshopVenueProvider;

    private final WorkshopVenueId venueId = new WorkshopVenueId("abc");

    @Test
    void handle_updatesCapacityAndDuration() {
        var venue = new WorkshopVenue();
        venue.setId(venueId);
        when(workshopVenueProvider.read(venueId)).thenReturn(Optional.of(venue));
        when(workshopVenueProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = new UpdateDefaultsOfWorkshopVenue(workshopVenueProvider).handle(
                new UpdateDefaultsOfWorkshopVenue.Command(venueId, 12, 150)
        );

        assertEquals(12, result.getDefaultCapacity());
        assertEquals(150, result.getDefaultDurationMinutes());
    }

    @Test
    void handle_throwsWhenCapacityIsMissing() {
        assertThrows(RuntimeException.class, () ->
                new UpdateDefaultsOfWorkshopVenue(workshopVenueProvider).handle(
                        new UpdateDefaultsOfWorkshopVenue.Command(venueId, null, 120)
                )
        );
        verifyNoInteractions(workshopVenueProvider);
    }

    @Test
    void handle_throwsWhenCapacityIsZero() {
        assertThrows(RuntimeException.class, () ->
                new UpdateDefaultsOfWorkshopVenue(workshopVenueProvider).handle(
                        new UpdateDefaultsOfWorkshopVenue.Command(venueId, 0, 120)
                )
        );
    }

    @Test
    void handle_throwsWhenCapacityIsAboveMaximum() {
        assertThrows(RuntimeException.class, () ->
                new UpdateDefaultsOfWorkshopVenue(workshopVenueProvider).handle(
                        new UpdateDefaultsOfWorkshopVenue.Command(venueId, UpdateDefaultsOfWorkshopVenue.MAX_CAPACITY + 1, 120)
                )
        );
    }

    @Test
    void handle_throwsWhenDurationIsTooShort() {
        assertThrows(RuntimeException.class, () ->
                new UpdateDefaultsOfWorkshopVenue(workshopVenueProvider).handle(
                        new UpdateDefaultsOfWorkshopVenue.Command(venueId, 8, UpdateDefaultsOfWorkshopVenue.MIN_DURATION_MINUTES - 1)
                )
        );
    }

    @Test
    void handle_throwsWhenDurationIsTooLong() {
        assertThrows(RuntimeException.class, () ->
                new UpdateDefaultsOfWorkshopVenue(workshopVenueProvider).handle(
                        new UpdateDefaultsOfWorkshopVenue.Command(venueId, 8, UpdateDefaultsOfWorkshopVenue.MAX_DURATION_MINUTES + 1)
                )
        );
    }

    @Test
    void handle_throwsWhenVenueNotFound() {
        when(workshopVenueProvider.read(venueId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                new UpdateDefaultsOfWorkshopVenue(workshopVenueProvider).handle(
                        new UpdateDefaultsOfWorkshopVenue.Command(venueId, 8, 120)
                )
        );
    }

}
