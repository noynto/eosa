package me.noynto.eosa.application;

import me.noynto.eosa.mail.Mail;
import me.noynto.eosa.mail.MailProvider;
import me.noynto.eosa.shared.WorkshopBookingId;
import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.shared.WorkshopVenueId;
import me.noynto.eosa.workshop.WorkshopBooking;
import me.noynto.eosa.workshop.WorkshopBookingState;
import me.noynto.eosa.workshop.WorkshopSession;
import me.noynto.eosa.workshop.WorkshopSessionProvider;
import me.noynto.eosa.workshop.WorkshopSessionState;
import me.noynto.eosa.workshop.WorkshopVenue;
import me.noynto.eosa.workshop.WorkshopVenueProvider;
import me.noynto.eosa.workshop.WorkshopWaitlistEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CancelWorkshopBookingByParticipantTest {

    @Mock WorkshopSessionProvider workshopSessionProvider;
    @Mock WorkshopVenueProvider workshopVenueProvider;
    @Mock MailProvider mailProvider;

    private final Instant startsAt = Instant.parse("2026-11-14T13:00:00Z");
    private final WorkshopSessionId sessionId = new WorkshopSessionId("session");
    private InMemoryWorkshopBookings bookings;
    private InMemoryWorkshopWaitlistEntries waitlist;
    private WorkshopBooking booking;

    @BeforeEach
    void setUp() {
        bookings = new InMemoryWorkshopBookings();
        waitlist = new InMemoryWorkshopWaitlistEntries();
        var session = new WorkshopSession();
        session.setId(sessionId);
        session.setVenueId(new WorkshopVenueId("venue"));
        session.setStartsAt(startsAt);
        session.setDurationMinutes(120);
        session.setCapacity(1);
        session.setState(WorkshopSessionState.SCHEDULED);
        var venue = new WorkshopVenue();
        venue.setName("Café des Arts");
        when(workshopSessionProvider.read(sessionId)).thenReturn(Optional.of(session));
        when(workshopVenueProvider.read(session.getVenueId())).thenReturn(Optional.of(venue));
        booking = new WorkshopBooking();
        booking.setId(new WorkshopBookingId("booking"));
        booking.setSessionId(sessionId);
        booking.setFirstName("Ada");
        booking.setEmail("ada@example.com");
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
        var mail = ArgumentCaptor.forClass(Mail.class);
        verify(mailProvider).send(mail.capture());
        assertEquals("ada@example.com", mail.getValue().to());
        assertEquals("workshop-booking-cancellation", mail.getValue().template());
        assertEquals(now, booking.getCancellationEmailSentAt());
    }

    @Test
    void handle_refusesLessThan48HoursBeforeTheStart() {
        var outcome = cancelAt(startsAt.minus(Duration.ofHours(47)));

        assertEquals(CancelWorkshopBookingByParticipant.Outcome.TOO_LATE, outcome);
        assertEquals(WorkshopBookingState.CONFIRMED, booking.getState());
        verify(mailProvider, never()).send(any());
    }

    @Test
    void handle_refusesExactly48HoursBeforeTheStart() {
        assertEquals(CancelWorkshopBookingByParticipant.Outcome.TOO_LATE, cancelAt(startsAt.minus(Duration.ofHours(48))));
    }

    @Test
    void handle_keepsTheCancellationWhenTheEmailCannotBeSent() {
        doThrow(new RuntimeException("SMTP")).when(mailProvider).send(any());

        var outcome = cancelAt(startsAt.minus(Duration.ofDays(3)));

        assertEquals(CancelWorkshopBookingByParticipant.Outcome.CANCELLED, outcome);
        assertEquals(WorkshopBookingState.CANCELLED, booking.getState());
        assertNull(booking.getCancellationEmailSentAt());
    }

    @Test
    void handle_reportsAnAlreadyCancelledBooking() {
        booking.setState(WorkshopBookingState.CANCELLED);

        assertEquals(CancelWorkshopBookingByParticipant.Outcome.ALREADY_CANCELLED, cancelAt(startsAt.minus(Duration.ofDays(3))));
        verify(mailProvider, never()).send(any());
    }

    @Test
    void handle_hasNothingToCancelForAnExpiredRequest() {
        booking.setState(WorkshopBookingState.EXPIRED);

        assertEquals(CancelWorkshopBookingByParticipant.Outcome.NOT_ACTIVE, cancelAt(startsAt.minus(Duration.ofDays(3))));
    }

    @Test
    void handle_throwsForAnUnknownToken() {
        assertThrows(RuntimeException.class, () -> useCase(Clock.systemUTC())
                .handle(new CancelWorkshopBookingByParticipant.Command("other")));
    }

    @Test
    void handle_warnsTheWaitlistThatAPlaceIsFree() {
        var rosa = new WorkshopWaitlistEntry();
        rosa.setSessionId(sessionId);
        rosa.setFirstName("Rosa");
        rosa.setEmail("rosa@example.com");
        rosa.setToken("token-rosa");
        rosa.setCreatedAt(startsAt.minus(Duration.ofDays(4)));
        waitlist.write(rosa);
        var now = startsAt.minus(Duration.ofDays(3));

        cancelAt(now);

        var mails = ArgumentCaptor.forClass(Mail.class);
        verify(mailProvider, times(2)).send(mails.capture());
        assertEquals("workshop-waitlist-place-available", mails.getAllValues().get(1).template());
        assertEquals("rosa@example.com", mails.getAllValues().get(1).to());
        assertEquals(now, rosa.getLastNotifiedAt());
    }

    private CancelWorkshopBookingByParticipant.Outcome cancelAt(Instant now) {
        return useCase(Clock.fixed(now, ZoneOffset.UTC)).handle(new CancelWorkshopBookingByParticipant.Command("token")).outcome();
    }

    private CancelWorkshopBookingByParticipant useCase(Clock clock) {
        return new CancelWorkshopBookingByParticipant(bookings, workshopSessionProvider, workshopVenueProvider, waitlist,
                mailProvider, clock, URI.create("https://eosa.me"));
    }

}
