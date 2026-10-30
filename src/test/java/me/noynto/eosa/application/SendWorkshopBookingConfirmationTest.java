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
class SendWorkshopBookingConfirmationTest {

    @Mock WorkshopSessionProvider workshopSessionProvider;
    @Mock WorkshopVenueProvider workshopVenueProvider;
    @Mock MailProvider mailProvider;

    private final Instant now = Instant.parse("2026-11-10T10:00:00Z");
    private final WorkshopBookingId bookingId = new WorkshopBookingId("booking");
    private InMemoryWorkshopBookings bookings;
    private WorkshopBooking booking;

    @BeforeEach
    void setUp() {
        bookings = new InMemoryWorkshopBookings();
        booking = new WorkshopBooking();
        booking.setId(bookingId);
        booking.setSessionId(new WorkshopSessionId("session"));
        booking.setFirstName("Marie");
        booking.setEmail("marie@example.com");
        booking.setState(WorkshopBookingState.CONFIRMED);
        booking.setToken("token");
        booking.setCreatedAt(now);
        bookings.write(booking);

        var session = new WorkshopSession();
        session.setId(new WorkshopSessionId("session"));
        session.setVenueId(new WorkshopVenueId("venue"));
        session.setStartsAt(Instant.parse("2026-11-14T13:00:00Z"));
        session.setDurationMinutes(120);
        session.setState(WorkshopSessionState.SCHEDULED);
        var venue = new WorkshopVenue();
        venue.setName("Café des Arts");
        venue.setAddress("12 rue Stanislas");
        venue.setPostalCode("54000");
        venue.setCity("Nancy");
        when(workshopSessionProvider.read(session.getId())).thenReturn(Optional.of(session));
        when(workshopVenueProvider.read(session.getVenueId())).thenReturn(Optional.of(venue));
    }

    @Test
    void handle_sendsTheRecapAndMarksItSent() {
        sendWorkshopBookingConfirmation().handle(new SendWorkshopBookingConfirmation.Command(bookingId));

        var mail = ArgumentCaptor.forClass(Mail.class);
        verify(mailProvider).send(mail.capture());
        assertEquals("workshop-booking-confirmation", mail.getValue().template());
        assertEquals("https://eosa.me/workshops/bookings/token/cancel", mail.getValue().model().get("cancelUrl"));
        // 48 h avant le samedi 14 novembre à 14 h 00.
        assertEquals("jeudi 12 novembre 2026 à 14 h 00", mail.getValue().model().get("cancellationDeadline"));
        assertEquals(now, booking.getConfirmationEmailSentAt());
    }

    @Test
    void handle_doesNotSendTwice() {
        booking.setConfirmationEmailSentAt(now);

        sendWorkshopBookingConfirmation().handle(new SendWorkshopBookingConfirmation.Command(bookingId));

        verify(mailProvider, never()).send(any());
    }

    @Test
    void handle_doesNotSendForAnUnconfirmedBooking() {
        booking.setState(WorkshopBookingState.PENDING);

        sendWorkshopBookingConfirmation().handle(new SendWorkshopBookingConfirmation.Command(bookingId));

        verify(mailProvider, never()).send(any());
    }

    @Test
    void handle_leavesItUnsentWhenSendingFails() {
        doThrow(new RuntimeException("SMTP")).when(mailProvider).send(any());

        assertThrows(RuntimeException.class, () ->
                sendWorkshopBookingConfirmation().handle(new SendWorkshopBookingConfirmation.Command(bookingId)));
        assertNull(booking.getConfirmationEmailSentAt());
    }

    private SendWorkshopBookingConfirmation sendWorkshopBookingConfirmation() {
        return new SendWorkshopBookingConfirmation(bookings, workshopSessionProvider, workshopVenueProvider, mailProvider,
                Clock.fixed(now, ZoneOffset.UTC), URI.create("https://eosa.me"));
    }

}
