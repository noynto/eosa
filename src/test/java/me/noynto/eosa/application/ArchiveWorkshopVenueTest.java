package me.noynto.eosa.application;

import me.noynto.eosa.shared.WorkshopVenueId;
import me.noynto.eosa.workshop.WorkshopVenue;
import me.noynto.eosa.workshop.WorkshopVenueProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArchiveWorkshopVenueTest {

    @Mock WorkshopVenueProvider workshopVenueProvider;

    private final WorkshopVenueId venueId = new WorkshopVenueId("abc");

    @Test
    void handle_marksVenueAsArchived() {
        var venue = new WorkshopVenue();
        venue.setId(venueId);
        when(workshopVenueProvider.read(venueId)).thenReturn(Optional.of(venue));
        when(workshopVenueProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = new ArchiveWorkshopVenue(workshopVenueProvider).handle(new ArchiveWorkshopVenue.Command(venueId));

        assertTrue(result.isArchived());
        verify(workshopVenueProvider).write(argThat(WorkshopVenue::isArchived));
    }

    @Test
    void handle_throwsWhenVenueIdIsNull() {
        assertThrows(RuntimeException.class, () ->
                new ArchiveWorkshopVenue(workshopVenueProvider).handle(new ArchiveWorkshopVenue.Command(null))
        );
    }

    @Test
    void handle_throwsWhenVenueNotFound() {
        when(workshopVenueProvider.read(venueId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                new ArchiveWorkshopVenue(workshopVenueProvider).handle(new ArchiveWorkshopVenue.Command(venueId))
        );
    }

}
