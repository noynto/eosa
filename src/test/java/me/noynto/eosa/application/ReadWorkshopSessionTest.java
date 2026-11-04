package me.noynto.eosa.application;

import me.noynto.eosa.shared.WorkshopBookingId;
import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.shared.WorkshopVenueId;
import me.noynto.eosa.workshop.WorkshopBooking;
import me.noynto.eosa.workshop.WorkshopBookingState;
import me.noynto.eosa.workshop.WorkshopSession;
import me.noynto.eosa.workshop.WorkshopSessionProvider;
import me.noynto.eosa.workshop.WorkshopVenue;
import me.noynto.eosa.workshop.WorkshopVenueProvider;
import me.noynto.eosa.workshop.WorkshopWaitlistEntry;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReadWorkshopSessionTest {

    @Mock WorkshopSessionProvider workshopSessionProvider;
    @Mock WorkshopVenueProvider workshopVenueProvider;

    private final Instant now = Instant.parse("2026-11-10T10:00:00Z");
    private final WorkshopSessionId sessionId = new WorkshopSessionId("session");
    private final InMemoryWorkshopBookings bookings = new InMemoryWorkshopBookings();
    private final InMemoryWorkshopWaitlistEntries waitlist = new InMemoryWorkshopWaitlistEntries();
    private WorkshopSession session;
    private WorkshopVenue venue;

    @BeforeEach
    void setUp() {
        session = new WorkshopSession();
        session.setId(sessionId);
        session.setVenueId(new WorkshopVenueId("venue"));
        venue = new WorkshopVenue();
        venue.setName("Café des Arts");
        when(workshopSessionProvider.read(sessionId)).thenReturn(Optional.of(session));
        when(workshopVenueProvider.read(session.getVenueId())).thenReturn(Optional.of(venue));
    }

    @Test
    void handle_returnsTheSessionItsVenueItsOccupiedPlacesAndItsWaitlist() {
        booking("confirmed", WorkshopBookingState.CONFIRMED, now.minus(Duration.ofDays(1)));
        booking("awaiting", WorkshopBookingState.PENDING, now.minus(Duration.ofMinutes(5)));
        booking("stale", WorkshopBookingState.PENDING, now.minus(Duration.ofHours(2)));
        booking("cancelled", WorkshopBookingState.CANCELLED, now.minus(Duration.ofDays(2)));
        waiting("rosa@example.com", null);
        waiting("gone@example.com", now.minus(Duration.ofHours(1)));

        var result = readWorkshopSession().handle(new ReadWorkshopSession.Command(sessionId));

        assertSame(session, result.session());
        assertSame(venue, result.venue());
        // La demande expirée et la réservation annulée ne comptent pas.
        assertEquals(2, result.occupiedPlaces());
        assertEquals(1, result.waitlistCount());
    }

    @Test
    void handle_throwsWhenTheSessionDoesNotExist() {
        var unknown = new WorkshopSessionId("unknown");
        when(workshopSessionProvider.read(unknown)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> readWorkshopSession().handle(new ReadWorkshopSession.Command(unknown)));
    }

    @Test
    void handle_requiresASessionId() {
        assertThrows(RuntimeException.class, () -> readWorkshopSession().handle(new ReadWorkshopSession.Command(null)));
    }

    private ReadWorkshopSession readWorkshopSession() {
        return new ReadWorkshopSession(workshopSessionProvider, workshopVenueProvider, bookings, waitlist,
                Clock.fixed(now, ZoneOffset.UTC));
    }

    private void booking(String id, WorkshopBookingState state, Instant createdAt) {
        var booking = new WorkshopBooking();
        booking.setId(new WorkshopBookingId(id));
        booking.setSessionId(sessionId);
        booking.setState(state);
        booking.setCreatedAt(createdAt);
        bookings.write(booking);
    }

    private void waiting(String email, Instant removedAt) {
        var entry = new WorkshopWaitlistEntry();
        entry.setSessionId(sessionId);
        entry.setEmail(email);
        entry.setCreatedAt(now.minus(Duration.ofDays(1)));
        entry.setRemovedAt(removedAt);
        waitlist.write(entry);
    }

}
