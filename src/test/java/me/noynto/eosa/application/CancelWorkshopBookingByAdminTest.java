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
class CancelWorkshopBookingByAdminTest {

    @Mock WorkshopSessionProvider workshopSessionProvider;
    @Mock WorkshopVenueProvider workshopVenueProvider;
    @Mock MailProvider mailProvider;

    private final Instant now = Instant.parse("2026-11-14T10:00:00Z");
    private final WorkshopSessionId sessionId = new WorkshopSessionId("session");
    private InMemoryWorkshopBookings bookings;
    private InMemoryWorkshopWaitlistEntries waitlist;
    private WorkshopSession session;
    private WorkshopBooking booking;

    @BeforeEach
    void setUp() {
        bookings = new InMemoryWorkshopBookings();
        waitlist = new InMemoryWorkshopWaitlistEntries();
        // Quelques heures avant l'atelier : seule l'administration peut encore annuler.
        session = new WorkshopSession();
        session.setId(sessionId);
        session.setVenueId(new WorkshopVenueId("venue"));
        session.setStartsAt(Instant.parse("2026-11-14T13:00:00Z"));
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
        booking.setCreatedAt(now.minus(Duration.ofDays(3)));
        bookings.write(booking);
    }

    @Test
    void handle_cancelsWithoutDeadlineAndSendsTheEmail() {
        cancelWorkshopBookingByAdmin().handle(new CancelWorkshopBookingByAdmin.Command(sessionId, booking.getId()));

        assertEquals(WorkshopBookingState.CANCELLED, booking.getState());
        assertEquals(now, booking.getCancelledAt());
        var mail = ArgumentCaptor.forClass(Mail.class);
        verify(mailProvider).send(mail.capture());
        assertEquals("ada@example.com", mail.getValue().to());
        assertEquals("workshop-booking-cancellation", mail.getValue().template());
        assertEquals(now, booking.getCancellationEmailSentAt());
    }

    @Test
    void handle_warnsTheWaitingListOfTheFreedPlace() {
        var rosa = waiting("rosa@example.com", "token-rosa");

        cancelWorkshopBookingByAdmin().handle(new CancelWorkshopBookingByAdmin.Command(sessionId, booking.getId()));

        var mails = ArgumentCaptor.forClass(Mail.class);
        verify(mailProvider, times(2)).send(mails.capture());
        assertEquals("workshop-waitlist-place-available", mails.getAllValues().get(1).template());
        assertEquals("rosa@example.com", mails.getAllValues().get(1).to());
        assertEquals(now, rosa.getLastNotifiedAt());
    }

    @Test
    void handle_doesNotWarnTheWaitingListOnceTheSessionHasStarted() {
        session.setStartsAt(now.minus(Duration.ofMinutes(5)));
        var rosa = waiting("rosa@example.com", "token-rosa");

        cancelWorkshopBookingByAdmin().handle(new CancelWorkshopBookingByAdmin.Command(sessionId, booking.getId()));

        verify(mailProvider, times(1)).send(any());
        assertNull(rosa.getLastNotifiedAt());
    }

    @Test
    void handle_keepsTheCancellationWhenTheEmailDoesNotLeave() {
        doThrow(new RuntimeException("SMTP")).when(mailProvider).send(any());

        cancelWorkshopBookingByAdmin().handle(new CancelWorkshopBookingByAdmin.Command(sessionId, booking.getId()));

        assertEquals(WorkshopBookingState.CANCELLED, booking.getState());
        assertNull(booking.getCancellationEmailSentAt());
    }

    @Test
    void handle_refusesABookingOfAnotherSession() {
        assertThrows(RuntimeException.class, () -> cancelWorkshopBookingByAdmin().handle(
                new CancelWorkshopBookingByAdmin.Command(new WorkshopSessionId("other"), booking.getId())));
        assertEquals(WorkshopBookingState.CONFIRMED, booking.getState());
    }

    @Test
    void handle_refusesAnInactiveBooking() {
        booking.setState(WorkshopBookingState.CANCELLED);

        assertThrows(RuntimeException.class, () -> cancelWorkshopBookingByAdmin().handle(
                new CancelWorkshopBookingByAdmin.Command(sessionId, booking.getId())));
        verify(mailProvider, never()).send(any());
    }

    private CancelWorkshopBookingByAdmin cancelWorkshopBookingByAdmin() {
        return new CancelWorkshopBookingByAdmin(bookings, workshopSessionProvider, workshopVenueProvider, waitlist,
                mailProvider, Clock.fixed(now, ZoneOffset.UTC), URI.create("https://eosa.me"));
    }

    private WorkshopWaitlistEntry waiting(String email, String token) {
        var entry = new WorkshopWaitlistEntry();
        entry.setSessionId(sessionId);
        entry.setFirstName(email.substring(0, email.indexOf('@')));
        entry.setEmail(email);
        entry.setToken(token);
        entry.setCreatedAt(now.minus(Duration.ofDays(1)));
        return waitlist.write(entry);
    }

}
