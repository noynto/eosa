package me.noynto.eosa.workshop;

import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.shared.WorkshopVenueId;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

public class WorkshopSession {

    /**
     * Fuseau de référence des ateliers : ouverture des réservations, délai d'annulation, affichage.
     */
    public static final ZoneId ZONE = ZoneId.of("Europe/Paris");

    /**
     * Les réservations ouvrent toujours 7 jours avant le début, à la même heure (heure de Paris).
     */
    public static final int BOOKING_OPENING_DAYS = 7;

    /**
     * Bornes de la capacité et de la durée d'une session, reprises pour les valeurs par défaut d'un lieu.
     */
    public static final int MAX_CAPACITY = 50;
    public static final int MIN_DURATION_MINUTES = 15;
    public static final int MAX_DURATION_MINUTES = 480;

    private WorkshopSessionId id;
    private WorkshopVenueId venueId;
    private Instant startsAt;
    private int durationMinutes;
    private int capacity;
    private WorkshopSessionState state;
    private Instant cancelledAt;
    /** Date à laquelle la liste d'attente a été prévenue de l'ouverture des réservations ; null avant. */
    private Instant openingAnnouncedAt;

    public WorkshopSession() {
    }

    public Instant getEndsAt() {
        return startsAt.plus(Duration.ofMinutes(durationMinutes));
    }

    public Instant getBookingOpensAt() {
        return startsAt.atZone(ZONE).minusDays(BOOKING_OPENING_DAYS).toInstant();
    }

    /**
     * @throws RuntimeException si la capacité n'est pas un nombre de places valide pour une session
     */
    public static void checkCapacity(Integer capacity) {
        if (capacity == null || capacity < 1 || capacity > MAX_CAPACITY) {
            throw new RuntimeException("La capacité doit être comprise entre 1 et " + MAX_CAPACITY + " places.");
        }
    }

    /**
     * @throws RuntimeException si la durée n'est pas une durée valide pour une session
     */
    public static void checkDuration(Integer durationMinutes) {
        if (durationMinutes == null || durationMinutes < MIN_DURATION_MINUTES || durationMinutes > MAX_DURATION_MINUTES) {
            throw new RuntimeException("La durée doit être comprise entre "
                    + MIN_DURATION_MINUTES + " et " + MAX_DURATION_MINUTES + " minutes.");
        }
    }

    /**
     * Les réservations sont ouvertes : session programmée, ouverture passée, début pas encore atteint.
     */
    public boolean isBookable(Instant now) {
        return state == WorkshopSessionState.SCHEDULED
                && !now.isBefore(getBookingOpensAt())
                && now.isBefore(startsAt);
    }

    public WorkshopSessionId getId() {
        return id;
    }

    public void setId(WorkshopSessionId id) {
        this.id = id;
    }

    public WorkshopVenueId getVenueId() {
        return venueId;
    }

    public void setVenueId(WorkshopVenueId venueId) {
        this.venueId = venueId;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(Instant startsAt) {
        this.startsAt = startsAt;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public WorkshopSessionState getState() {
        return state;
    }

    public void setState(WorkshopSessionState state) {
        this.state = state;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public Instant getOpeningAnnouncedAt() {
        return openingAnnouncedAt;
    }

    public void setOpeningAnnouncedAt(Instant openingAnnouncedAt) {
        this.openingAnnouncedAt = openingAnnouncedAt;
    }
}
