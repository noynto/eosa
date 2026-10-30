package me.noynto.eosa.application;

import me.noynto.eosa.image.Image;
import me.noynto.eosa.image.ImageProvider;
import me.noynto.eosa.shared.ImageId;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddImageToWorkshopVenueTest {

    @Mock WorkshopVenueProvider workshopVenueProvider;
    @Mock ImageProvider imageProvider;

    private final WorkshopVenueId venueId = new WorkshopVenueId("abc");

    @Test
    void handle_uploadsImageAndAttachesItToVenue() {
        var venue = new WorkshopVenue();
        venue.setId(venueId);
        var uploaded = new Image();
        uploaded.setId(new ImageId("img-1"));
        when(workshopVenueProvider.read(venueId)).thenReturn(Optional.of(venue));
        when(imageProvider.upload(any())).thenReturn(uploaded);
        when(workshopVenueProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = new AddImageToWorkshopVenue(workshopVenueProvider, imageProvider).handle(
                new AddImageToWorkshopVenue.Command(venueId, new Image())
        );

        assertEquals(new ImageId("img-1"), result.getImageId());
    }

    @Test
    void handle_throwsWhenImageIsNull() {
        assertThrows(RuntimeException.class, () ->
                new AddImageToWorkshopVenue(workshopVenueProvider, imageProvider).handle(
                        new AddImageToWorkshopVenue.Command(venueId, null)
                )
        );
        verify(imageProvider, never()).upload(any());
    }

    @Test
    void handle_throwsWithoutUploadingWhenVenueNotFound() {
        when(workshopVenueProvider.read(venueId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                new AddImageToWorkshopVenue(workshopVenueProvider, imageProvider).handle(
                        new AddImageToWorkshopVenue.Command(venueId, new Image())
                )
        );
        verify(imageProvider, never()).upload(any());
    }

}
