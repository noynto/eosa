package me.noynto.eosa.service;

import me.noynto.eosa.workshop.WorkshopBooking;
import me.noynto.eosa.workshop.WorkshopBookingState;
import me.noynto.eosa.workshop.WorkshopSession;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkshopPlacesTest {

    private final Instant now = Instant.parse("2026-11-10T10:00:00Z");

    @Test
    void occupied_countsTheBookingsThatHoldAPlace() {
        var bookings = List.of(
                booking(WorkshopBookingState.CONFIRMED, now.minus(Duration.ofDays(2))),
                booking(WorkshopBookingState.ATTENDED, now.minus(Duration.ofDays(2))),
                booking(WorkshopBookingState.NO_SHOW, now.minus(Duration.ofDays(2))),
                booking(WorkshopBookingState.PENDING, now.minus(Duration.ofMinutes(10))),
                booking(WorkshopBookingState.PENDING, now.minus(Duration.ofMinutes(40))),
                booking(WorkshopBookingState.CANCELLED, now.minus(Duration.ofDays(2))),
                booking(WorkshopBookingState.EXPIRED, now.minus(Duration.ofDays(2)))
        );

        // Seule la demande encore dans son délai de confirmation compte, avec les trois réservations confirmées.
        assertEquals(4, WorkshopPlaces.occupied(bookings, now));
    }

    @Test
    void free_subtractsTheOccupiedPlacesFromTheCapacity() {
        var bookings = List.of(booking(WorkshopBookingState.CONFIRMED, now.minus(Duration.ofDays(1))));

        assertEquals(1, WorkshopPlaces.free(sessionOfCapacity(2), bookings, now));
    }

    @Test
    void free_neverGoesBelowZero() {
        var bookings = List.of(
                booking(WorkshopBookingState.CONFIRMED, now.minus(Duration.ofDays(1))),
                booking(WorkshopBookingState.CONFIRMED, now.minus(Duration.ofDays(1)))
        );

        assertEquals(0, WorkshopPlaces.free(sessionOfCapacity(1), bookings, now));
    }

    private static WorkshopBooking booking(WorkshopBookingState state, Instant createdAt) {
        var booking = new WorkshopBooking();
        booking.setState(state);
        booking.setCreatedAt(createdAt);
        return booking;
    }

    private static WorkshopSession sessionOfCapacity(int capacity) {
        var session = new WorkshopSession();
        session.setCapacity(capacity);
        return session;
    }

}
