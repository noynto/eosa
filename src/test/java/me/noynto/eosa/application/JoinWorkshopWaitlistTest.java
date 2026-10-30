package me.noynto.eosa.application;

import me.noynto.eosa.shared.WorkshopBookingId;
import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.workshop.WorkshopBooking;
import me.noynto.eosa.workshop.WorkshopBookingState;
import me.noynto.eosa.workshop.WorkshopSession;
import me.noynto.eosa.workshop.WorkshopSessionProvider;
import me.noynto.eosa.workshop.WorkshopSessionState;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JoinWorkshopWaitlistTest {

    @Mock WorkshopSessionProvider workshopSessionProvider;

    private final Instant now = Instant.parse("2026-11-10T10:00:00Z");
    private final WorkshopSessionId sessionId = new WorkshopSessionId("session");
    private InMemoryWorkshopBookings bookings;
    private InMemoryWorkshopWaitlistEntries waitlist;
    private WorkshopSession session;

    @BeforeEach
    void setUp() {
        bookings = new InMemoryWorkshopBookings();
        waitlist = new InMemoryWorkshopWaitlistEntries();
        session = new WorkshopSession();
        session.setId(sessionId);
        session.setStartsAt(Instant.parse("2026-11-14T13:00:00Z"));
        session.setDurationMinutes(120);
        session.setCapacity(1);
        session.setState(WorkshopSessionState.SCHEDULED);
        when(workshopSessionProvider.read(sessionId)).thenReturn(Optional.of(session));
        booking("ada@example.com", WorkshopBookingState.CONFIRMED);
    }

    @Test
    void handle_addsAWaitingEntryWhenTheSessionIsFull() {
        var entry = joinWorkshopWaitlist().handle(new JoinWorkshopWaitlist.Command(sessionId, " Rosa ", "rosa@example.com"));

        assertEquals("Rosa", entry.getFirstName());
        assertTrue(entry.isWaiting());
        assertNotNull(entry.getToken());
        assertEquals(now, entry.getCreatedAt());
    }

    @Test
    void handle_keepsTheExistingEntryForTheSameEmail() {
        var first = joinWorkshopWaitlist().handle(new JoinWorkshopWaitlist.Command(sessionId, "Rosa", "rosa@example.com"));

        var second = joinWorkshopWaitlist().handle(new JoinWorkshopWaitlist.Command(sessionId, "Rosa", "ROSA@example.com"));

        assertSame(first, second);
        assertEquals(1, waitlist.all().size());
    }

    @Test
    void handle_acceptsAnEntryBeforeTheBookingOpeningEvenWithFreePlaces() {
        // Atelier le 28 novembre : réservations ouvertes le 21, on est le 10.
        session.setStartsAt(Instant.parse("2026-11-28T13:00:00Z"));
        session.setCapacity(8);

        var entry = joinWorkshopWaitlist().handle(new JoinWorkshopWaitlist.Command(sessionId, "Rosa", "rosa@example.com"));

        assertTrue(entry.isWaiting());
    }

    @Test
    void handle_refusesAStartedSession() {
        session.setStartsAt(now.minus(Duration.ofMinutes(5)));

        assertThrows(RuntimeException.class, () ->
                joinWorkshopWaitlist().handle(new JoinWorkshopWaitlist.Command(sessionId, "Rosa", "rosa@example.com")));
    }

    @Test
    void handle_refusesWhenPlacesAreAvailable() {
        session.setCapacity(2);

        assertThrows(RuntimeException.class, () ->
                joinWorkshopWaitlist().handle(new JoinWorkshopWaitlist.Command(sessionId, "Rosa", "rosa@example.com")));
    }

    @Test
    void handle_refusesSomeoneWhoAlreadyHasAPlace() {
        assertThrows(RuntimeException.class, () ->
                joinWorkshopWaitlist().handle(new JoinWorkshopWaitlist.Command(sessionId, "Ada", "ada@example.com")));
    }

    @Test
    void handle_refusesAnEmailMarkedAsNoShow() {
        var noShow = booking("rosa@example.com", WorkshopBookingState.NO_SHOW);
        noShow.setSessionId(new WorkshopSessionId("an-old-session"));

        assertThrows(RuntimeException.class, () ->
                joinWorkshopWaitlist().handle(new JoinWorkshopWaitlist.Command(sessionId, "Rosa", "rosa@example.com")));
    }

    @Test
    void handle_refusesAnInvalidEmail() {
        assertThrows(RuntimeException.class, () ->
                joinWorkshopWaitlist().handle(new JoinWorkshopWaitlist.Command(sessionId, "Rosa", "rosa")));
    }

    private JoinWorkshopWaitlist joinWorkshopWaitlist() {
        return new JoinWorkshopWaitlist(workshopSessionProvider, bookings, waitlist, Clock.fixed(now, ZoneOffset.UTC));
    }

    private WorkshopBooking booking(String email, WorkshopBookingState state) {
        var booking = new WorkshopBooking();
        booking.setId(new WorkshopBookingId(email));
        booking.setSessionId(sessionId);
        booking.setEmail(email);
        booking.setState(state);
        booking.setCreatedAt(now.minus(Duration.ofDays(1)));
        return bookings.write(booking);
    }

}
