package me.noynto.eosa.workshop;

import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.shared.WorkshopWaitlistEntryId;

import java.time.Instant;

/**
 * Inscription sur la liste d'attente d'une session complète : la personne est prévenue par e-mail
 * quand une place se libère.
 */
public class WorkshopWaitlistEntry {
    private WorkshopWaitlistEntryId id;
    private WorkshopSessionId sessionId;
    private String firstName;
    private String email;
    /** Jeton secret du lien de désinscription envoyé par e-mail. */
    private String token;
    private Instant createdAt;
    private Instant lastNotifiedAt;
    /** Date de sortie de la liste (désinscription, place réservée, session annulée) ; null tant qu'elle attend. */
    private Instant removedAt;

    public WorkshopWaitlistEntry() {
    }

    public boolean isWaiting() {
        return removedAt == null;
    }

    public WorkshopWaitlistEntryId getId() {
        return id;
    }

    public void setId(WorkshopWaitlistEntryId id) {
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public Instant getLastNotifiedAt() {
        return lastNotifiedAt;
    }

    public void setLastNotifiedAt(Instant lastNotifiedAt) {
        this.lastNotifiedAt = lastNotifiedAt;
    }

    public Instant getRemovedAt() {
        return removedAt;
    }

    public void setRemovedAt(Instant removedAt) {
        this.removedAt = removedAt;
    }
}
