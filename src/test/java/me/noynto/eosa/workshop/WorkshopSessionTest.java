package me.noynto.eosa.workshop;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkshopSessionTest {

    @Test
    void bookingOpensFourteenDaysBeforeAtTheSameParisTime() {
        var session = sessionStartingAt(LocalDateTime.of(2026, 11, 14, 14, 0));

        assertEquals(
                LocalDateTime.of(2026, 10, 31, 14, 0),
                LocalDateTime.ofInstant(session.getBookingOpensAt(), WorkshopSession.ZONE)
        );
    }

    @Test
    void bookingOpeningKeepsTheParisTimeAcrossDaylightSavingChange() {
        // Passage à l'heure d'hiver le 25 octobre 2026, entre l'ouverture et le début : 14 jours ≠ 336 heures.
        var session = sessionStartingAt(LocalDateTime.of(2026, 10, 29, 14, 0));

        assertEquals(
                LocalDateTime.of(2026, 10, 15, 14, 0),
                LocalDateTime.ofInstant(session.getBookingOpensAt(), WorkshopSession.ZONE)
        );
    }

    @Test
    void endsAtAddsTheDuration() {
        var session = new WorkshopSession();
        session.setStartsAt(Instant.parse("2026-11-14T13:00:00Z"));
        session.setDurationMinutes(150);

        assertEquals(Instant.parse("2026-11-14T15:30:00Z"), session.getEndsAt());
    }

    @Test
    void isBookableBetweenTheOpeningAndTheStart() {
        var session = sessionStartingAt(LocalDateTime.of(2026, 11, 14, 14, 0));
        session.setState(WorkshopSessionState.SCHEDULED);

        assertFalse(session.isBookable(session.getBookingOpensAt().minusSeconds(1)));
        assertTrue(session.isBookable(session.getBookingOpensAt()));
        assertTrue(session.isBookable(session.getStartsAt().minusSeconds(1)));
        assertFalse(session.isBookable(session.getStartsAt()));
    }

    @Test
    void isNotBookableOnceCancelled() {
        var session = sessionStartingAt(LocalDateTime.of(2026, 11, 14, 14, 0));
        session.setState(WorkshopSessionState.CANCELLED);

        assertFalse(session.isBookable(session.getBookingOpensAt().plusSeconds(1)));
    }

    @Test
    void checkCapacityAcceptsOneToTheMaximum() {
        assertDoesNotThrow(() -> WorkshopSession.checkCapacity(1));
        assertDoesNotThrow(() -> WorkshopSession.checkCapacity(WorkshopSession.MAX_CAPACITY));
    }

    @Test
    void checkCapacityRefusesNothingZeroAndMoreThanTheMaximum() {
        assertThrows(RuntimeException.class, () -> WorkshopSession.checkCapacity(null));
        assertThrows(RuntimeException.class, () -> WorkshopSession.checkCapacity(0));
        assertThrows(RuntimeException.class, () -> WorkshopSession.checkCapacity(WorkshopSession.MAX_CAPACITY + 1));
    }

    @Test
    void checkDurationAcceptsTheBoundsAndRefusesWhatIsOutside() {
        assertDoesNotThrow(() -> WorkshopSession.checkDuration(WorkshopSession.MIN_DURATION_MINUTES));
        assertDoesNotThrow(() -> WorkshopSession.checkDuration(WorkshopSession.MAX_DURATION_MINUTES));
        assertThrows(RuntimeException.class, () -> WorkshopSession.checkDuration(null));
        assertThrows(RuntimeException.class, () -> WorkshopSession.checkDuration(WorkshopSession.MIN_DURATION_MINUTES - 1));
        assertThrows(RuntimeException.class, () -> WorkshopSession.checkDuration(WorkshopSession.MAX_DURATION_MINUTES + 1));
    }

    private WorkshopSession sessionStartingAt(LocalDateTime parisTime) {
        var session = new WorkshopSession();
        session.setStartsAt(parisTime.atZone(WorkshopSession.ZONE).toInstant());
        return session;
    }

}
