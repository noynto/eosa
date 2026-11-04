package me.noynto.eosa.workshop;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkshopSessionTest {

    @Test
    void bookingOpensSevenDaysBeforeAtTheSameParisTime() {
        var session = sessionStartingAt(LocalDateTime.of(2026, 11, 14, 14, 0));

        assertEquals(
                LocalDateTime.of(2026, 11, 7, 14, 0),
                LocalDateTime.ofInstant(session.getBookingOpensAt(), WorkshopSession.ZONE)
        );
    }

    @Test
    void bookingOpeningKeepsTheParisTimeAcrossDaylightSavingChange() {
        // Passage à l'heure d'hiver le 25 octobre 2026 : 7 jours ≠ 168 heures.
        var session = sessionStartingAt(LocalDateTime.of(2026, 10, 29, 14, 0));

        assertEquals(
                LocalDateTime.of(2026, 10, 22, 14, 0),
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

    private WorkshopSession sessionStartingAt(LocalDateTime parisTime) {
        var session = new WorkshopSession();
        session.setStartsAt(parisTime.atZone(WorkshopSession.ZONE).toInstant());
        return session;
    }

}
