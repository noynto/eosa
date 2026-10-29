package me.noynto.eosa.infrastructure.web;

import me.noynto.eosa.application.ReadWorkshopSessions;
import me.noynto.eosa.workshop.WorkshopSession;
import me.noynto.eosa.workshop.WorkshopSessionState;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * Mise en forme des dates de session pour les templates (Mustache n'a pas de logique) :
 * toujours en heure de Paris, en français.
 */
final class WorkshopSessionFormat {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("H 'h' mm", Locale.FRENCH);
    private static final DateTimeFormatter DATE_INPUT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_INPUT = DateTimeFormatter.ofPattern("HH:mm");

    private WorkshopSessionFormat() {
    }

    /** « Samedi 14 novembre 2026 » */
    static String date(Instant instant) {
        String date = DATE.format(instant.atZone(WorkshopSession.ZONE));
        return Character.toUpperCase(date.charAt(0)) + date.substring(1);
    }

    /** « 14 h 00 » */
    static String time(Instant instant) {
        return TIME.format(instant.atZone(WorkshopSession.ZONE));
    }

    /** « 14 h 00 – 16 h 00 » */
    static String timeRange(WorkshopSession session) {
        return time(session.getStartsAt()) + " – " + time(session.getEndsAt());
    }

    /** Valeur d'un champ {@code <input type="date">}. */
    static String dateInput(Instant instant) {
        return DATE_INPUT.format(instant.atZone(WorkshopSession.ZONE));
    }

    /** Valeur d'un champ {@code <input type="time">}. */
    static String timeInput(Instant instant) {
        return TIME_INPUT.format(instant.atZone(WorkshopSession.ZONE));
    }

    /** Date et heure saisies dans les champs date et heure, ou null si elles sont invalides. */
    static LocalDateTime parseInputs(String date, String time) {
        try {
            return LocalDateTime.of(LocalDate.parse(date, DATE_INPUT), LocalTime.parse(time, TIME_INPUT));
        } catch (DateTimeParseException | NullPointerException e) {
            return null;
        }
    }

    static Integer parseInteger(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** État lisible d'une session à l'instant donné, pour l'administration. */
    static String status(ReadWorkshopSessions.Overview overview, Instant now) {
        WorkshopSession session = overview.session();
        if (session.getState() == WorkshopSessionState.CANCELLED) {
            return "Annulée";
        }
        if (now.isBefore(session.getBookingOpensAt())) {
            return "Ouverture des réservations le " + lowerFirst(date(session.getBookingOpensAt()))
                    + " à " + time(session.getBookingOpensAt());
        }
        if (now.isBefore(session.getStartsAt())) {
            return overview.occupiedPlaces() >= session.getCapacity() ? "Complet" : "Réservations ouvertes";
        }
        if (now.isBefore(session.getEndsAt())) {
            return "En cours";
        }
        return "Terminée";
    }

    private static String lowerFirst(String value) {
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }

}
