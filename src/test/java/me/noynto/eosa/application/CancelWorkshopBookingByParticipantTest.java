package me.noynto.eosa.application;

import me.noynto.eosa.shared.WorkshopBookingId;
import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.workshop.WorkshopBooking;
import me.noynto.eosa.workshop.WorkshopBookingState;
import me.noynto.eosa.workshop.WorkshopSession;
import me.noynto.eosa.workshop.WorkshopSessionProvider;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CancelWorkshopBookingByParticipantTest {

    @Mock WorkshopSessionProvider workshopSessionProvider;
    @Mock SendWorkshopBookingCancellation sendWorkshopBookingCancellation;

    private final Instant startsAt = Instant.parse("2026-11-14T13:00:00Z");
    private final WorkshopSessionId sessionId = new WorkshopSessionId("session");
    private InMemoryWorkshopBookings bookings;
    private WorkshopBooking booking;

    @BeforeEach
    void setUp() {
        bookings = new InMemoryWorkshopBookings();
        var session = new WorkshopSession();
        session.setId(sessionId);
        session.setStartsAt(startsAt);
        session.setDurationMinutes(120);
        when(workshopSessionProvider.read(sessionId)).thenReturn(Optional.of(session));
        booking = new WorkshopBooking();
        booking.setId(new WorkshopBookingId("booking"));
        booking.setSessionId(sessionId);
        booking.setState(WorkshopBookingState.CONFIRMED);
        booking.setToken("token");
        booking.setCreatedAt(startsAt.minus(Duration.ofDays(5)));
        bookings.write(booking);
    }

    @Test
    void handle_cancelsMoreThan48HoursBeforeTheStartAndSendsTheEmail() {
        var now = startsAt.minus(Duration.ofHours(49));

        var outcome = cancelAt(now);

        assertEquals(CancelWorkshopBookingByParticipant.Outcome.CANCELLED, outcome);
        assertEquals(WorkshopBookingState.CANCELLED, booking.getState());
        assertEquals(now, booking.getCancelledAt());
        verify(sendWorkshopBookingCancellation).handle(new SendWorkshopBookingCancellation.Command(booking.getId()));
    }

    @Test
    void handle_refusesLessThan48HoursBeforeTheStart() {
        var outcome = cancelAt(startsAt.minus(Duration.ofHours(47)));

        assertEquals(CancelWorkshopBookingByParticipant.Outcome.TOO_LATE, outcome);
        assertEquals(WorkshopBookingState.CONFIRMED, booking.getState());
        verify(sendWorkshopBookingCancellation, never()).handle(any());
    }

    @Test
    void handle_refusesExactly48HoursBeforeTheStart() {
        assertEquals(CancelWorkshopBookingByParticipant.Outcome.TOO_LATE, cancelAt(startsAt.minus(Duration.ofHours(48))));
    }

    @Test
    void handle_keepsTheCancellationWhenTheEmailCannotBeSent() {
        doThrow(new RuntimeException("SMTP")).when(sendWorkshopBookingCancellation).handle(any());

        var outcome = cancelAt(startsAt.minus(Duration.ofDays(3)));

        assertEquals(CancelWorkshopBookingByParticipant.Outcome.CANCELLED, outcome);
        assertEquals(WorkshopBookingState.CANCELLED, booking.getState());
    }

    @Test
    void handle_reportsAnAlreadyCancelledBooking() {
        booking.setState(WorkshopBookingState.CANCELLED);

        assertEquals(CancelWorkshopBookingByParticipant.Outcome.ALREADY_CANCELLED, cancelAt(startsAt.minus(Duration.ofDays(3))));
        verify(sendWorkshopBookingCancellation, never()).handle(any());
    }

    @Test
    void handle_hasNothingToCancelForAnExpiredRequest() {
        booking.setState(WorkshopBookingState.EXPIRED);

        assertEquals(CancelWorkshopBookingByParticipant.Outcome.NOT_ACTIVE, cancelAt(startsAt.minus(Duration.ofDays(3))));
    }

    @Test
    void handle_throwsForAnUnknownToken() {
        assertThrows(RuntimeException.class, () -> new CancelWorkshopBookingByParticipant(bookings, workshopSessionProvider,
                sendWorkshopBookingCancellation, Clock.systemUTC()).handle(new CancelWorkshopBookingByParticipant.Command("other")));
    }

    private CancelWorkshopBookingByParticipant.Outcome cancelAt(Instant now) {
        return new CancelWorkshopBookingByParticipant(bookings, workshopSessionProvider, sendWorkshopBookingCancellation,
                Clock.fixed(now, ZoneOffset.UTC)).handle(new CancelWorkshopBookingByParticipant.Command("token"));
    }

}
