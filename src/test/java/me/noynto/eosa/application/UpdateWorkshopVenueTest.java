package me.noynto.eosa.application;

import me.noynto.eosa.shared.WorkshopVenueId;
import me.noynto.eosa.workshop.WorkshopVenue;
import me.noynto.eosa.workshop.WorkshopVenueProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateWorkshopVenueTest {

    @Mock WorkshopVenueProvider workshopVenueProvider;

    private final WorkshopVenueId venueId = new WorkshopVenueId("abc");

    @Test
    void handle_updatesAllInformation() {
        when(workshopVenueProvider.read(venueId)).thenReturn(Optional.of(venueWith(venueId)));
        when(workshopVenueProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = new UpdateWorkshopVenue(workshopVenueProvider).handle(new UpdateWorkshopVenue.Command(
                venueId, "Café des Arts", " Un café cosy. ", "12 rue Stanislas", "54000", "Nancy",
                "https://www.instagram.com/cafedesarts"
        ));

        assertEquals("Café des Arts", result.getName());
        assertEquals("Un café cosy.", result.getDescription());
        assertEquals("12 rue Stanislas", result.getAddress());
        assertEquals("54000", result.getPostalCode());
        assertEquals("Nancy", result.getCity());
        assertEquals("https://www.instagram.com/cafedesarts", result.getLinkUrl());
    }

    @Test
    void handle_clearsBlankOptionalFields() {
        var venue = venueWith(venueId);
        venue.setDescription("Ancienne présentation");
        venue.setLinkUrl("https://example.com");
        when(workshopVenueProvider.read(venueId)).thenReturn(Optional.of(venue));
        when(workshopVenueProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = new UpdateWorkshopVenue(workshopVenueProvider).handle(new UpdateWorkshopVenue.Command(
                venueId, "Café des Arts", "  ", "12 rue Stanislas", "54000", "Nancy", ""
        ));

        assertNull(result.getDescription());
        assertNull(result.getLinkUrl());
    }

    @ParameterizedTest
    @ValueSource(strings = {"javascript:alert(1)", "www.instagram.com/cafe", "ftp://example.com", "https://"})
    void handle_throwsWhenLinkIsNotAWebUrl(String linkUrl) {
        assertThrows(RuntimeException.class, () ->
                new UpdateWorkshopVenue(workshopVenueProvider).handle(new UpdateWorkshopVenue.Command(
                        venueId, "Café des Arts", null, "12 rue Stanislas", "54000", "Nancy", linkUrl
                ))
        );
        verify(workshopVenueProvider, never()).write(any());
    }

    @Test
    void handle_throwsWhenNameIsBlank() {
        assertThrows(RuntimeException.class, () ->
                new UpdateWorkshopVenue(workshopVenueProvider).handle(new UpdateWorkshopVenue.Command(
                        venueId, " ", null, "12 rue Stanislas", "54000", "Nancy", null
                ))
        );
    }

    @Test
    void handle_throwsWhenVenueIdIsNull() {
        assertThrows(RuntimeException.class, () ->
                new UpdateWorkshopVenue(workshopVenueProvider).handle(new UpdateWorkshopVenue.Command(
                        null, "Café des Arts", null, "12 rue Stanislas", "54000", "Nancy", null
                ))
        );
    }

    @Test
    void handle_throwsWhenVenueNotFound() {
        when(workshopVenueProvider.read(venueId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                new UpdateWorkshopVenue(workshopVenueProvider).handle(new UpdateWorkshopVenue.Command(
                        venueId, "Café des Arts", null, "12 rue Stanislas", "54000", "Nancy", null
                ))
        );
    }

    private WorkshopVenue venueWith(WorkshopVenueId id) {
        var venue = new WorkshopVenue();
        venue.setId(id);
        return venue;
    }

}
