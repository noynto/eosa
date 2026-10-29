package me.noynto.eosa.application;

import me.noynto.eosa.shared.WorkshopBookingId;
import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.workshop.WorkshopBooking;
import me.noynto.eosa.workshop.WorkshopBookingState;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExpirePendingWorkshopBookingsTest {

    private final Instant now = Instant.parse("2026-11-10T10:00:00Z");
    private final InMemoryWorkshopBookings bookings = new InMemoryWorkshopBookings();

    @Test
    void handle_expiresOnlyPendingBookingsPastTheConfirmationDelay() {
        var stale = booking("stale", WorkshopBookingState.PENDING, now.minus(Duration.ofMinutes(31)));
        var recent = booking("recent", WorkshopBookingState.PENDING, now.minus(Duration.ofMinutes(29)));
        var confirmed = booking("confirmed", WorkshopBookingState.CONFIRMED, now.minus(Duration.ofDays(1)));

        int expired = new ExpirePendingWorkshopBookings(bookings, Clock.fixed(now, ZoneOffset.UTC)).handle();

        assertEquals(1, expired);
        assertEquals(WorkshopBookingState.EXPIRED, stale.getState());
        assertEquals(WorkshopBookingState.PENDING, recent.getState());
        assertEquals(WorkshopBookingState.CONFIRMED, confirmed.getState());
    }

    private WorkshopBooking booking(String id, WorkshopBookingState state, Instant createdAt) {
        var booking = new WorkshopBooking();
        booking.setId(new WorkshopBookingId(id));
        booking.setSessionId(new WorkshopSessionId("session"));
        booking.setState(state);
        booking.setCreatedAt(createdAt);
        return bookings.write(booking);
    }

}
