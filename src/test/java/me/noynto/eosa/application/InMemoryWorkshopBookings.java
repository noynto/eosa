package me.noynto.eosa.application;

import me.noynto.eosa.shared.WorkshopBookingId;
import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.workshop.WorkshopBooking;
import me.noynto.eosa.workshop.WorkshopBookingProvider;
import me.noynto.eosa.workshop.WorkshopBookingState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Réservations en mémoire pour les tests, avec les mêmes filtres et le même ordre que la persistance JDBC.
 */
class InMemoryWorkshopBookings implements WorkshopBookingProvider {

    private final List<WorkshopBooking> bookings = new ArrayList<>();

    @Override
    public Stream<WorkshopBookingId> readIds(WorkshopSessionId sessionId, Set<WorkshopBookingState> states,
                                             String email, String token) {
        return bookings.stream()
                .filter(booking -> sessionId == null || sessionId.equals(booking.getSessionId()))
                .filter(booking -> states == null || states.isEmpty() || states.contains(booking.getState()))
                .filter(booking -> email == null || email.equalsIgnoreCase(booking.getEmail()))
                .filter(booking -> token == null || token.equals(booking.getToken()))
                .sorted(Comparator.comparing(WorkshopBooking::getCreatedAt).thenComparing(booking -> booking.getId().value()))
                .map(WorkshopBooking::getId)
                .toList()
                .stream();
    }

    @Override
    public Optional<WorkshopBooking> read(WorkshopBookingId workshopBookingId) {
        return bookings.stream().filter(booking -> booking.getId().equals(workshopBookingId)).findFirst();
    }

    @Override
    public WorkshopBooking write(WorkshopBooking workshopBooking) {
        if (workshopBooking.getId() == null) {
            workshopBooking.setId(new WorkshopBookingId(UUID.randomUUID().toString()));
        }
        bookings.removeIf(booking -> booking.getId().equals(workshopBooking.getId()));
        bookings.add(workshopBooking);
        return workshopBooking;
    }

    List<WorkshopBooking> all() {
        return bookings;
    }

}
