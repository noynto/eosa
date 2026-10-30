package me.noynto.eosa.application;

import me.noynto.eosa.workshop.WorkshopVenueProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateWorkshopVenueTest {

    @Mock WorkshopVenueProvider workshopVenueProvider;

    @Test
    void handle_createsVenueWithStrippedFieldsAndDefaults() {
        when(workshopVenueProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = new CreateWorkshopVenue(workshopVenueProvider).handle(
                new CreateWorkshopVenue.Command(" Café des Arts ", "12 rue Stanislas", "54000", "Nancy")
        );

        assertEquals("Café des Arts", result.getName());
        assertEquals("12 rue Stanislas", result.getAddress());
        assertEquals("54000", result.getPostalCode());
        assertEquals("Nancy", result.getCity());
        assertEquals(CreateWorkshopVenue.DEFAULT_CAPACITY, result.getDefaultCapacity());
        assertEquals(CreateWorkshopVenue.DEFAULT_DURATION_MINUTES, result.getDefaultDurationMinutes());
        assertFalse(result.isArchived());
        assertNull(result.getDescription());
        assertNull(result.getLinkUrl());
    }

    @Test
    void handle_throwsWhenNameIsBlank() {
        assertThrows(RuntimeException.class, () ->
                new CreateWorkshopVenue(workshopVenueProvider).handle(
                        new CreateWorkshopVenue.Command("  ", "12 rue Stanislas", "54000", "Nancy")
                )
        );
        verifyNoInteractions(workshopVenueProvider);
    }

    @Test
    void handle_throwsWhenAddressIsMissing() {
        assertThrows(RuntimeException.class, () ->
                new CreateWorkshopVenue(workshopVenueProvider).handle(
                        new CreateWorkshopVenue.Command("Café des Arts", null, "54000", "Nancy")
                )
        );
    }

    @Test
    void handle_throwsWhenPostalCodeIsMissing() {
        assertThrows(RuntimeException.class, () ->
                new CreateWorkshopVenue(workshopVenueProvider).handle(
                        new CreateWorkshopVenue.Command("Café des Arts", "12 rue Stanislas", "", "Nancy")
                )
        );
    }

    @Test
    void handle_throwsWhenCityIsMissing() {
        assertThrows(RuntimeException.class, () ->
                new CreateWorkshopVenue(workshopVenueProvider).handle(
                        new CreateWorkshopVenue.Command("Café des Arts", "12 rue Stanislas", "54000", null)
                )
        );
    }

}
