package me.noynto.eosa.workshop;

import me.noynto.eosa.shared.WorkshopBookingId;
import me.noynto.eosa.shared.WorkshopSessionId;

import java.time.Duration;
import java.time.Instant;

public class WorkshopBooking {

    /**
     * Délai pour confirmer sa place depuis l'e-mail : pendant ce temps, la réservation PENDING retient
     * sa place ; au-delà, la place est libérée.
     */
    public static final Duration CONFIRMATION_DELAY = Duration.ofMinutes(30);

    /**
     * La participante peut annuler elle-même jusqu'à ce délai avant le début ; au-delà, elle contacte EOSA.
     */
    public static final Duration SELF_CANCELLATION_DEADLINE = Duration.ofHours(48);

    /**
     * Tolérance pour les retards : une participante ne peut être marquée absente qu'à partir de ce délai
     * après le début de la séance.
     */
    public static final Duration NO_SHOW_GRACE_PERIOD = Duration.ofMinutes(30);

    private WorkshopBookingId id;
    private WorkshopSessionId sessionId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private WorkshopBookingState state;
    /** Jeton secret des liens de confirmation et d'annulation envoyés par e-mail. */
    private String token;
    private Instant createdAt;
    private Instant confirmedAt;
    private Instant cancelledAt;
    private Instant confirmationEmailSentAt;
    private Instant cancellationEmailSentAt;
    private Instant reminderEmailSentAt;

    public WorkshopBooking() {
    }

    /**
     * Réservation encore valable (en attente de confirmation ou confirmée), donc annulable.
     */
    public boolean isActive(Instant now) {
        return state == WorkshopBookingState.CONFIRMED || isAwaitingConfirmation(now);
    }

    /**
     * Une réservation PENDING n'occupe sa place que pendant le délai de confirmation.
     */
    public boolean isAwaitingConfirmation(Instant now) {
        return state == WorkshopBookingState.PENDING && now.isBefore(createdAt.plus(CONFIRMATION_DELAY));
    }

    /**
     * Occupe une place de la session : en attente de confirmation dans le délai, confirmée, ou déjà
     * passée en présente/absente.
     */
    public boolean occupiesAPlace(Instant now) {
        return switch (state) {
            case PENDING -> isAwaitingConfirmation(now);
            case CONFIRMED, ATTENDED, NO_SHOW -> true;
            case CANCELLED, EXPIRED -> false;
        };
    }

    public WorkshopBookingId getId() {
        return id;
    }

    public void setId(WorkshopBookingId id) {
        this.id = id;
    }

    public WorkshopSessionId getSessionId() {
        return sessionId;
    }

    public void setSessionId(WorkshopSessionId sessionId) {
        this.sessionId = sessionId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public WorkshopBookingState getState() {
        return state;
    }

    public void setState(WorkshopBookingState state) {
        this.state = state;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(Instant confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public Instant getConfirmationEmailSentAt() {
        return confirmationEmailSentAt;
    }

    public void setConfirmationEmailSentAt(Instant confirmationEmailSentAt) {
        this.confirmationEmailSentAt = confirmationEmailSentAt;
    }

    public Instant getCancellationEmailSentAt() {
        return cancellationEmailSentAt;
    }

    public void setCancellationEmailSentAt(Instant cancellationEmailSentAt) {
        this.cancellationEmailSentAt = cancellationEmailSentAt;
    }

    public Instant getReminderEmailSentAt() {
        return reminderEmailSentAt;
    }

    public void setReminderEmailSentAt(Instant reminderEmailSentAt) {
        this.reminderEmailSentAt = reminderEmailSentAt;
    }
}
