package me.noynto.eosa.application;

import me.noynto.eosa.shared.WorkshopBookingId;
import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.workshop.WorkshopBooking;
import me.noynto.eosa.workshop.WorkshopBookingState;
import me.noynto.eosa.workshop.WorkshopSession;
import me.noynto.eosa.workshop.WorkshopSessionProvider;
import me.noynto.eosa.workshop.WorkshopSessionState;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PurgeWorkshopPersonalDataTest {

    @Mock WorkshopSessionProvider workshopSessionProvider;

    // Atelier passé : samedi 14 novembre 2026, 14 h 00 – 16 h 00 à Paris.
    private final WorkshopSessionId pastSessionId = new WorkshopSessionId("past");
    private final Instant pastStartsAt = Instant.parse("2026-11-14T13:00:00Z");
    private final Instant pastEndsAt = Instant.parse("2026-11-14T15:00:00Z");
    // Atelier à venir.
    private final WorkshopSessionId upcomingSessionId = new WorkshopSessionId("upcoming");
    private InMemoryWorkshopBookings bookings;
    private InMemoryWorkshopWaitlistEntries waitlist;
    private WorkshopSession upcoming;

    @BeforeEach
    void setUp() {
        bookings = new InMemoryWorkshopBookings();
        waitlist = new InMemoryWorkshopWaitlistEntries();
        when(workshopSessionProvider.read(pastSessionId)).thenReturn(Optional.of(session(pastSessionId, pastStartsAt)));
        upcoming = session(upcomingSessionId, Instant.parse("2028-01-10T13:00:00Z"));
        when(workshopSessionProvider.read(upcomingSessionId)).thenReturn(Optional.of(upcoming));
    }

    @Test
    void handle_deletesBookingsOneYearAfterTheWorkshopIncludingNoShows() {
        booking("attended", pastSessionId, WorkshopBookingState.ATTENDED);
        booking("no-show", pastSessionId, WorkshopBookingState.NO_SHOW);
        booking("future", upcomingSessionId, WorkshopBookingState.CONFIRMED);

        // Un an jour pour jour après la fin de l'atelier.
        var result = purgeAt(Instant.parse("2027-11-14T15:00:00Z"));

        assertEquals(2, result.deletedBookings());
        assertEquals(1, bookings.all().size());
        assertEquals("future", bookings.all().getFirst().getId().value());
    }

    @Test
    void handle_keepsBookingsDuringTheYearAfterTheWorkshop() {
        booking("no-show", pastSessionId, WorkshopBookingState.NO_SHOW);

        var result = purgeAt(Instant.parse("2027-11-14T14:59:00Z"));

        assertEquals(0, result.deletedBookings());
        assertEquals(1, bookings.all().size());
    }

    @Test
    void handle_deletesWaitlistEntriesOnceTheWorkshopIsOver() {
        entry("past-waiting", pastSessionId, null);

        var result = purgeAt(pastEndsAt.plus(Duration.ofMinutes(1)));

        assertEquals(1, result.deletedWaitlistEntries());
        assertTrue(waitlist.all().isEmpty());
    }

    @Test
    void handle_deletesWaitlistEntriesThatNoLongerWaitAndThoseOfCancelledSessions() {
        var now = Instant.parse("2027-01-10T10:00:00Z");
        entry("left", upcomingSessionId, now.minus(Duration.ofDays(1)));
        entry("waiting", upcomingSessionId, null);

        assertEquals(1, purgeAt(now).deletedWaitlistEntries());
        assertEquals("waiting", waitlist.all().getFirst().getFirstName());

        upcoming.setState(WorkshopSessionState.CANCELLED);
        assertEquals(1, purgeAt(now).deletedWaitlistEntries());
        assertTrue(waitlist.all().isEmpty());
    }

    private PurgeWorkshopPersonalData.Result purgeAt(Instant now) {
        return new PurgeWorkshopPersonalData(workshopSessionProvider, bookings, waitlist, Clock.fixed(now, ZoneOffset.UTC)).handle();
    }

    private WorkshopSession session(WorkshopSessionId id, Instant startsAt) {
        var session = new WorkshopSession();
        session.setId(id);
        session.setStartsAt(startsAt);
        session.setDurationMinutes(120);
        session.setState(WorkshopSessionState.SCHEDULED);
        return session;
    }

    private void booking(String id, WorkshopSessionId sessionId, WorkshopBookingState state) {
        var booking = new WorkshopBooking();
        booking.setId(new WorkshopBookingId(id));
        booking.setSessionId(sessionId);
        booking.setState(state);
        booking.setCreatedAt(pastStartsAt.minus(Duration.ofDays(3)));
        bookings.write(booking);
    }

    private void entry(String firstName, WorkshopSessionId sessionId, Instant removedAt) {
        var entry = new WorkshopWaitlistEntry();
        entry.setSessionId(sessionId);
        entry.setFirstName(firstName);
        entry.setCreatedAt(pastStartsAt.minus(Duration.ofDays(3)).plus(Duration.ofMinutes(waitlist.all().size())));
        entry.setRemovedAt(removedAt);
        waitlist.write(entry);
    }

}
