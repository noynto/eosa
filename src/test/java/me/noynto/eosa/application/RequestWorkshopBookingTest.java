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
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RequestWorkshopBookingTest {

    @Mock WorkshopSessionProvider workshopSessionProvider;
    @Mock WorkshopVenueProvider workshopVenueProvider;
    @Mock MailProvider mailProvider;

    private final Instant now = Instant.parse("2026-11-10T10:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private final WorkshopSessionId sessionId = new WorkshopSessionId("session");
    private final WorkshopVenueId venueId = new WorkshopVenueId("venue");
    private InMemoryWorkshopBookings bookings;
    private WorkshopSession session;

    @BeforeEach
    void setUp() {
        bookings = new InMemoryWorkshopBookings();
        session = new WorkshopSession();
        session.setId(sessionId);
        session.setVenueId(venueId);
        // Samedi 14 novembre 2026, 14 h 00 à Paris : réservations ouvertes depuis le 7 novembre.
        session.setStartsAt(Instant.parse("2026-11-14T13:00:00Z"));
        session.setDurationMinutes(120);
        session.setCapacity(2);
        session.setState(WorkshopSessionState.SCHEDULED);
        var venue = new WorkshopVenue();
        venue.setId(venueId);
        venue.setName("Café des Arts");
        venue.setAddress("12 rue Stanislas");
        venue.setPostalCode("54000");
        venue.setCity("Nancy");
        when(workshopSessionProvider.read(sessionId)).thenReturn(Optional.of(session));
        when(workshopVenueProvider.read(venueId)).thenReturn(Optional.of(venue));
    }

    @Test
    void handle_holdsThePlaceAndSendsTheConfirmationLink() {
        WorkshopBooking result = requestWorkshopBooking().handle(command(" Marie ", "Curie", "Marie@Example.com"));

        assertEquals(WorkshopBookingState.PENDING, result.getState());
        assertEquals("Marie", result.getFirstName());
        assertEquals(now, result.getCreatedAt());
        assertNotNull(result.getToken());
        assertTrue(result.getToken().length() >= 40, "le jeton n'est pas devinable");
        var mail = ArgumentCaptor.forClass(Mail.class);
        verify(mailProvider).send(mail.capture());
        assertEquals("Marie@Example.com", mail.getValue().to());
        assertEquals("workshop-booking-request", mail.getValue().template());
        assertEquals("https://eosa.me/workshops/bookings/" + result.getToken() + "/confirm", mail.getValue().model().get("confirmUrl"));
    }

    @Test
    void handle_refusesWhenTheSessionIsFull() {
        existingBooking("a@example.com", WorkshopBookingState.CONFIRMED, now.minus(Duration.ofDays(1)));
        existingBooking("b@example.com", WorkshopBookingState.PENDING, now.minus(Duration.ofMinutes(5)));

        assertThrows(RuntimeException.class, () -> requestWorkshopBooking().handle(command("Marie", "Curie", "marie@example.com")));
        verify(mailProvider, never()).send(any());
    }

    @Test
    void handle_countsAPendingBookingOnlyDuringTheConfirmationDelay() {
        existingBooking("a@example.com", WorkshopBookingState.CONFIRMED, now.minus(Duration.ofDays(1)));
        existingBooking("b@example.com", WorkshopBookingState.PENDING, now.minus(Duration.ofMinutes(31)));

        WorkshopBooking result = requestWorkshopBooking().handle(command("Marie", "Curie", "marie@example.com"));

        assertEquals(WorkshopBookingState.PENDING, result.getState());
    }

    @Test
    void handle_withdrawsTheLatestBookingWhenSimultaneousRequestsExceedTheCapacity() {
        // Une autre demande enregistrée au même moment, juste avant celle-ci, a pris la dernière place.
        existingBooking("a@example.com", WorkshopBookingState.CONFIRMED, now.minus(Duration.ofDays(1)));
        existingBooking("b@example.com", WorkshopBookingState.PENDING, now.minus(Duration.ofSeconds(1)));
        session.setCapacity(3);
        // Entre la vérification et l'écriture, une troisième demande passe devant.
        var concurrent = existingBooking("c@example.com", WorkshopBookingState.PENDING, now.minus(Duration.ofMillis(1)));

        assertThrows(RuntimeException.class, () -> requestWorkshopBooking().handle(command("Marie", "Curie", "marie@example.com")));
        var mine = bookings.all().stream().filter(booking -> "marie@example.com".equals(booking.getEmail())).findFirst().orElseThrow();
        assertEquals(WorkshopBookingState.EXPIRED, mine.getState());
        assertEquals(WorkshopBookingState.PENDING, concurrent.getState());
    }

    @Test
    void handle_refusesASecondBookingForTheSameEmail() {
        existingBooking("marie@example.com", WorkshopBookingState.CONFIRMED, now.minus(Duration.ofDays(1)));

        assertThrows(RuntimeException.class, () -> requestWorkshopBooking().handle(command("Marie", "Curie", "MARIE@example.com")));
    }

    @Test
    void handle_replacesAnUnconfirmedRequestOlderThanTheDelay() {
        var old = existingBooking("marie@example.com", WorkshopBookingState.PENDING, now.minus(Duration.ofMinutes(45)));

        WorkshopBooking result = requestWorkshopBooking().handle(command("Marie", "Curie", "marie@example.com"));

        assertEquals(WorkshopBookingState.EXPIRED, old.getState());
        assertEquals(WorkshopBookingState.PENDING, result.getState());
    }

    @Test
    void handle_refusesAnEmailMarkedAsNoShow() {
        var noShow = existingBooking("marie@example.com", WorkshopBookingState.NO_SHOW, now.minus(Duration.ofDays(30)));
        noShow.setSessionId(new WorkshopSessionId("an-old-session"));

        assertThrows(RuntimeException.class, () -> requestWorkshopBooking().handle(command("Marie", "Curie", "marie@example.com")));
        verify(mailProvider, never()).send(any());
    }

    @Test
    void handle_releasesThePlaceWhenTheEmailCannotBeSent() {
        doThrow(new RuntimeException("SMTP")).when(mailProvider).send(any());

        assertThrows(RuntimeException.class, () -> requestWorkshopBooking().handle(command("Marie", "Curie", "marie@example.com")));
        assertEquals(WorkshopBookingState.EXPIRED, bookings.all().getFirst().getState());
    }

    @Test
    void handle_refusesBeforeTheBookingOpening() {
        session.setStartsAt(Instant.parse("2026-11-28T13:00:00Z"));

        assertThrows(RuntimeException.class, () -> requestWorkshopBooking().handle(command("Marie", "Curie", "marie@example.com")));
    }

    @Test
    void handle_refusesAStartedSession() {
        session.setStartsAt(now.minus(Duration.ofMinutes(10)));

        assertThrows(RuntimeException.class, () -> requestWorkshopBooking().handle(command("Marie", "Curie", "marie@example.com")));
    }

    @Test
    void handle_refusesACancelledSession() {
        session.setState(WorkshopSessionState.CANCELLED);

        assertThrows(RuntimeException.class, () -> requestWorkshopBooking().handle(command("Marie", "Curie", "marie@example.com")));
    }

    @Test
    void handle_refusesWithoutAcceptedConditions() {
        assertThrows(RuntimeException.class, () -> requestWorkshopBooking().handle(new RequestWorkshopBooking.Command(
                sessionId, "Marie", "Curie", "marie@example.com", null, false
        )));
    }

    @Test
    void handle_refusesAnInvalidEmail() {
        assertThrows(RuntimeException.class, () -> requestWorkshopBooking().handle(command("Marie", "Curie", "marie@example")));
    }

    @Test
    void handle_refusesAnInvalidPhone() {
        assertThrows(RuntimeException.class, () -> requestWorkshopBooking().handle(new RequestWorkshopBooking.Command(
                sessionId, "Marie", "Curie", "marie@example.com", "<script>", true
        )));
    }

    private RequestWorkshopBooking requestWorkshopBooking() {
        return new RequestWorkshopBooking(workshopSessionProvider, workshopVenueProvider, bookings, mailProvider,
                clock, URI.create("https://eosa.me"));
    }

    private RequestWorkshopBooking.Command command(String firstName, String lastName, String email) {
        return new RequestWorkshopBooking.Command(sessionId, firstName, lastName, email, "06 12 34 56 78", true);
    }

    private WorkshopBooking existingBooking(String email, WorkshopBookingState state, Instant createdAt) {
        var booking = new WorkshopBooking();
        booking.setId(new WorkshopBookingId(email));
        booking.setSessionId(sessionId);
        booking.setFirstName("Inscrite");
        booking.setLastName("Existante");
        booking.setEmail(email);
        booking.setState(state);
        booking.setToken("token-" + email);
        booking.setCreatedAt(createdAt);
        return bookings.write(booking);
    }

}
