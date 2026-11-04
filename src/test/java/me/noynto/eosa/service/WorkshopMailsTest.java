package me.noynto.eosa.service;

import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.workshop.WorkshopBooking;
import me.noynto.eosa.workshop.WorkshopSession;
import me.noynto.eosa.workshop.WorkshopSessionState;
import me.noynto.eosa.workshop.WorkshopVenue;
import me.noynto.eosa.workshop.WorkshopWaitlistEntry;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkshopMailsTest {

    private final URI baseUrl = URI.create("https://eosa.me");
    private final WorkshopSession session = session();
    private final WorkshopVenue venue = venue();

    @Test
    void bookingConfirmation_carriesThePlaceTheScheduleAndTheCancelLink() {
        var mail = WorkshopMails.bookingConfirmation(booking(), session, venue, baseUrl);

        assertEquals("ada@example.com", mail.to());
        assertEquals("workshop-booking-confirmation", mail.template());
        assertEquals("Votre place est réservée — Atelier EOSA chez Café des Arts", mail.subject());
        assertEquals("Ada", mail.model().get("firstName"));
        assertEquals("12 rue des Arts, 54000 Nancy", mail.model().get("venueAddress"));
        assertEquals("https://eosa.me/workshops/bookings/token-ada/cancel", mail.model().get("cancelUrl"));
        assertTrue(mail.model().get("mapUrl").toString().startsWith("https://www.google.com/maps/search/?api=1&query=12+rue"));
    }

    @Test
    void bookingCancellation_saysTheBookingIsCancelledWhenTheSessionStillStands() {
        var mail = WorkshopMails.bookingCancellation(booking(), session, venue, baseUrl);

        assertEquals("Réservation annulée — Atelier EOSA chez Café des Arts", mail.subject());
        assertEquals(false, mail.model().get("sessionCancelled"));
        assertEquals("https://eosa.me/workshops", mail.model().get("workshopsUrl"));
    }

    @Test
    void bookingCancellation_saysTheSessionIsCancelledWhenItIs() {
        session.setState(WorkshopSessionState.CANCELLED);

        var mail = WorkshopMails.bookingCancellation(booking(), session, venue, baseUrl);

        assertEquals("Atelier annulé — Atelier EOSA chez Café des Arts", mail.subject());
        assertEquals(true, mail.model().get("sessionCancelled"));
    }

    @Test
    void waitlistSessionCancellation_isMarkedAsForTheWaitingList() {
        var mail = WorkshopMails.waitlistSessionCancellation(entry(), session, venue, baseUrl);

        assertEquals("rosa@example.com", mail.to());
        assertEquals("workshop-booking-cancellation", mail.template());
        assertEquals(true, mail.model().get("sessionCancelled"));
        assertEquals(true, mail.model().get("waitlisted"));
    }

    @Test
    void waitlistPlaceAvailable_linksToTheSessionAndToLeavingTheList() {
        var mail = WorkshopMails.waitlistPlaceAvailable(entry(), session, venue, baseUrl);

        assertEquals("rosa@example.com", mail.to());
        assertEquals("workshop-waitlist-place-available", mail.template());
        assertEquals("Une place s'est libérée — Atelier EOSA chez Café des Arts", mail.subject());
        assertEquals("https://eosa.me/workshops/sessions/session", mail.model().get("sessionUrl"));
        assertEquals("https://eosa.me/workshops/waitlist/token-rosa/leave", mail.model().get("leaveUrl"));
    }

    private static WorkshopSession session() {
        var session = new WorkshopSession();
        session.setId(new WorkshopSessionId("session"));
        session.setStartsAt(Instant.parse("2026-11-14T13:00:00Z"));
        session.setDurationMinutes(120);
        session.setState(WorkshopSessionState.SCHEDULED);
        return session;
    }

    private static WorkshopVenue venue() {
        var venue = new WorkshopVenue();
        venue.setName("Café des Arts");
        venue.setAddress("12 rue des Arts");
        venue.setPostalCode("54000");
        venue.setCity("Nancy");
        return venue;
    }

    private static WorkshopBooking booking() {
        var booking = new WorkshopBooking();
        booking.setFirstName("Ada");
        booking.setEmail("ada@example.com");
        booking.setToken("token-ada");
        return booking;
    }

    private static WorkshopWaitlistEntry entry() {
        var entry = new WorkshopWaitlistEntry();
        entry.setFirstName("Rosa");
        entry.setEmail("rosa@example.com");
        entry.setToken("token-rosa");
        return entry;
    }

}
