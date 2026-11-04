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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConfirmWorkshopBookingTest {

    @Mock WorkshopSessionProvider workshopSessionProvider;
    @Mock WorkshopVenueProvider workshopVenueProvider;
    @Mock MailProvider mailProvider;

    private final Instant now = Instant.parse("2026-11-10T10:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private final WorkshopSessionId sessionId = new WorkshopSessionId("session");
    private InMemoryWorkshopBookings bookings;
    private InMemoryWorkshopWaitlistEntries waitlist;

    @BeforeEach
    void setUp() {
        bookings = new InMemoryWorkshopBookings();
        waitlist = new InMemoryWorkshopWaitlistEntries();
        var session = new WorkshopSession();
        session.setId(sessionId);
        session.setVenueId(new WorkshopVenueId("venue"));
        session.setStartsAt(Instant.parse("2026-11-14T13:00:00Z"));
        session.setDurationMinutes(120);
        session.setState(WorkshopSessionState.SCHEDULED);
        var venue = new WorkshopVenue();
        venue.setName("Café des Arts");
        venue.setAddress("12 rue des Arts");
        venue.setPostalCode("54000");
        venue.setCity("Nancy");
        when(workshopSessionProvider.read(sessionId)).thenReturn(Optional.of(session));
        when(workshopVenueProvider.read(session.getVenueId())).thenReturn(Optional.of(venue));
    }

    @Test
    void handle_removesTheParticipantFromTheSessionWaitlist() {
        booking(WorkshopBookingState.PENDING, now.minus(Duration.ofMinutes(10)));
        var entry = new WorkshopWaitlistEntry();
        entry.setSessionId(sessionId);
        entry.setEmail("MARIE@example.com");
        entry.setCreatedAt(now.minus(Duration.ofDays(1)));
        waitlist.write(entry);

        confirmWorkshopBooking().handle(new ConfirmWorkshopBooking.Command("token"));

        assertEquals(now, entry.getRemovedAt());
    }

    @Test
    void handle_confirmsAPendingBookingWithinTheDelayAndSendsTheRecap() {
        var booking = booking(WorkshopBookingState.PENDING, now.minus(Duration.ofMinutes(10)));

        var outcome = confirmWorkshopBooking().handle(new ConfirmWorkshopBooking.Command("token")).outcome();

        assertEquals(ConfirmWorkshopBooking.Outcome.CONFIRMED, outcome);
        assertEquals(WorkshopBookingState.CONFIRMED, booking.getState());
        assertEquals(now, booking.getConfirmedAt());
        var mail = ArgumentCaptor.forClass(Mail.class);
        verify(mailProvider).send(mail.capture());
        assertEquals("marie@example.com", mail.getValue().to());
        assertEquals("workshop-booking-confirmation", mail.getValue().template());
        assertEquals(now, booking.getConfirmationEmailSentAt());
    }

    @Test
    void handle_keepsTheConfirmationWhenTheRecapCannotBeSent() {
        var booking = booking(WorkshopBookingState.PENDING, now.minus(Duration.ofMinutes(10)));
        doThrow(new RuntimeException("SMTP")).when(mailProvider).send(any());

        var outcome = confirmWorkshopBooking().handle(new ConfirmWorkshopBooking.Command("token")).outcome();

        assertEquals(ConfirmWorkshopBooking.Outcome.CONFIRMED, outcome);
        assertEquals(WorkshopBookingState.CONFIRMED, booking.getState());
        assertNull(booking.getConfirmationEmailSentAt());
    }

    @Test
    void handle_expiresAPendingBookingPastTheDelay() {
        var booking = booking(WorkshopBookingState.PENDING, now.minus(Duration.ofMinutes(31)));

        var outcome = confirmWorkshopBooking().handle(new ConfirmWorkshopBooking.Command("token")).outcome();

        assertEquals(ConfirmWorkshopBooking.Outcome.EXPIRED, outcome);
        assertEquals(WorkshopBookingState.EXPIRED, booking.getState());
        verify(mailProvider, never()).send(any());
    }

    @Test
    void handle_doesNothingMoreWhenAlreadyConfirmed() {
        var booking = booking(WorkshopBookingState.CONFIRMED, now.minus(Duration.ofMinutes(10)));
        booking.setConfirmedAt(now.minus(Duration.ofMinutes(5)));

        var outcome = confirmWorkshopBooking().handle(new ConfirmWorkshopBooking.Command("token")).outcome();

        assertEquals(ConfirmWorkshopBooking.Outcome.ALREADY_CONFIRMED, outcome);
        assertEquals(now.minus(Duration.ofMinutes(5)), booking.getConfirmedAt());
        verify(mailProvider, never()).send(any());
    }

    @Test
    void handle_doesNotConfirmACancelledBooking() {
        var booking = booking(WorkshopBookingState.CANCELLED, now.minus(Duration.ofMinutes(10)));

        var outcome = confirmWorkshopBooking().handle(new ConfirmWorkshopBooking.Command("token")).outcome();

        assertEquals(ConfirmWorkshopBooking.Outcome.CANCELLED, outcome);
        assertEquals(WorkshopBookingState.CANCELLED, booking.getState());
    }

    @Test
    void handle_throwsForAnUnknownToken() {
        booking(WorkshopBookingState.PENDING, now);

        assertThrows(RuntimeException.class, () -> confirmWorkshopBooking().handle(new ConfirmWorkshopBooking.Command("other")));
    }

    private ConfirmWorkshopBooking confirmWorkshopBooking() {
        return new ConfirmWorkshopBooking(bookings, workshopSessionProvider, workshopVenueProvider, waitlist, mailProvider,
                clock, URI.create("https://eosa.me"));
    }

    private WorkshopBooking booking(WorkshopBookingState state, Instant createdAt) {
        var booking = new WorkshopBooking();
        booking.setId(new WorkshopBookingId("booking"));
        booking.setSessionId(sessionId);
        booking.setFirstName("Marie");
        booking.setEmail("marie@example.com");
        booking.setState(state);
        booking.setToken("token");
        booking.setCreatedAt(createdAt);
        return bookings.write(booking);
    }

}
