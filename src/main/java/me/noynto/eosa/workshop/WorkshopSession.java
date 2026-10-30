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
