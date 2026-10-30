package me.noynto.eosa.application;

/**
 * Bornes communes à la capacité et à la durée des sessions, et aux valeurs par défaut des lieux.
 */
final class WorkshopSessionRules {

    static final int MAX_CAPACITY = 50;
    static final int MIN_DURATION_MINUTES = 15;
    static final int MAX_DURATION_MINUTES = 480;

    private WorkshopSessionRules() {
    }

    static void checkCapacity(Integer capacity) {
        if (capacity == null || capacity < 1 || capacity > MAX_CAPACITY) {
            throw new RuntimeException("La capacité doit être comprise entre 1 et " + MAX_CAPACITY + " places.");
        }
    }

    static void checkDuration(Integer durationMinutes) {
        if (durationMinutes == null || durationMinutes < MIN_DURATION_MINUTES || durationMinutes > MAX_DURATION_MINUTES) {
            throw new RuntimeException("La durée doit être comprise entre "
                    + MIN_DURATION_MINUTES + " et " + MAX_DURATION_MINUTES + " minutes.");
        }
    }

}
