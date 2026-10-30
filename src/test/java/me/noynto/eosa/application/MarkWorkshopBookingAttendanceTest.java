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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Pointage après l'atelier : présente, absente, tout le monde présent.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MarkWorkshopBookingAttendanceTest {

    @Mock WorkshopSessionProvider workshopSessionProvider;

    private final Instant startsAt = Instant.parse("2026-11-14T13:00:00Z");
    private final WorkshopSessionId sessionId = new WorkshopSessionId("session");
    private InMemoryWorkshopBookings bookings;
    private WorkshopSession session;
    private WorkshopBooking marie;

    @BeforeEach
    void setUp() {
        bookings = new InMemoryWorkshopBookings();
        session = new WorkshopSession();
        session.setId(sessionId);
        session.setStartsAt(startsAt);
        session.setDurationMinutes(120);
        session.setState(WorkshopSessionState.SCHEDULED);
        when(workshopSessionProvider.read(sessionId)).thenReturn(Optional.of(session));
        marie = booking("marie", WorkshopBookingState.CONFIRMED);
    }

    @Test
    void attended_isPossibleFromTheStart() {
        attendedAt(startsAt, marie);

        assertEquals(WorkshopBookingState.ATTENDED, marie.getState());
    }

    @Test
    void attended_isRefusedBeforeTheStart() {
        assertThrows(RuntimeException.class, () -> attendedAt(startsAt.minus(Duration.ofMinutes(1)), marie));
        assertEquals(WorkshopBookingState.CONFIRMED, marie.getState());
    }

    @Test
    void attended_correctsANoShowMarkedByMistake() {
        marie.setState(WorkshopBookingState.NO_SHOW);

        attendedAt(startsAt.plus(Duration.ofHours(3)), marie);

        assertEquals(WorkshopBookingState.ATTENDED, marie.getState());
    }

    @Test
    void attended_isRefusedForACancelledBooking() {
        marie.setState(WorkshopBookingState.CANCELLED);

        assertThrows(RuntimeException.class, () -> attendedAt(startsAt.plus(Duration.ofMinutes(5)), marie));
    }

    @Test
    void noShow_isRefusedDuringTheThirtyMinutesGracePeriod() {
        assertThrows(RuntimeException.class, () -> noShowAt(startsAt.plus(Duration.ofMinutes(29)), marie));
        assertEquals(WorkshopBookingState.CONFIRMED, marie.getState());
    }

    @Test
    void noShow_isPossibleThirtyMinutesAfterTheStart() {
        noShowAt(startsAt.plus(Duration.ofMinutes(30)), marie);

        assertEquals(WorkshopBookingState.NO_SHOW, marie.getState());
    }

    @Test
    void noShow_isRefusedForABookingOfAnotherSession() {
        var other = booking("other", WorkshopBookingState.CONFIRMED);
        other.setSessionId(new WorkshopSessionId("other-session"));

        assertThrows(RuntimeException.class, () -> noShowAt(startsAt.plus(Duration.ofHours(1)), other));
    }

    @Test
    void allAttended_marksOnlyTheBookingsNotYetMarked() {
        var absent = booking("absent", WorkshopBookingState.NO_SHOW);
        var cancelled = booking("cancelled", WorkshopBookingState.CANCELLED);

        int marked = new MarkAllWorkshopBookingsAttended(bookings, workshopSessionProvider,
                Clock.fixed(startsAt.plus(Duration.ofHours(2)), ZoneOffset.UTC)).handle(new MarkAllWorkshopBookingsAttended.Command(sessionId));

        assertEquals(1, marked);
        assertEquals(WorkshopBookingState.ATTENDED, marie.getState());
        assertEquals(WorkshopBookingState.NO_SHOW, absent.getState());
        assertEquals(WorkshopBookingState.CANCELLED, cancelled.getState());
    }

    @Test
    void allAttended_isRefusedForACancelledSession() {
        session.setState(WorkshopSessionState.CANCELLED);

        assertThrows(RuntimeException.class, () -> new MarkAllWorkshopBookingsAttended(bookings, workshopSessionProvider,
                Clock.fixed(startsAt.plus(Duration.ofHours(2)), ZoneOffset.UTC)).handle(new MarkAllWorkshopBookingsAttended.Command(sessionId)));
    }

    private void attendedAt(Instant now, WorkshopBooking booking) {
        new MarkWorkshopBookingAttended(bookings, workshopSessionProvider, Clock.fixed(now, ZoneOffset.UTC))
                .handle(new MarkWorkshopBookingAttended.Command(sessionId, booking.getId()));
    }

    private void noShowAt(Instant now, WorkshopBooking booking) {
        new MarkWorkshopBookingNoShow(bookings, workshopSessionProvider, Clock.fixed(now, ZoneOffset.UTC))
                .handle(new MarkWorkshopBookingNoShow.Command(sessionId, booking.getId()));
    }

    private WorkshopBooking booking(String id, WorkshopBookingState state) {
        var booking = new WorkshopBooking();
        booking.setId(new WorkshopBookingId(id));
        booking.setSessionId(sessionId);
        booking.setEmail(id + "@example.com");
        booking.setState(state);
        booking.setCreatedAt(startsAt.minus(Duration.ofDays(3)));
        return bookings.write(booking);
    }

}
