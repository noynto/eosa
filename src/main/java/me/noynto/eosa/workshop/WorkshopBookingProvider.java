package me.noynto.eosa.workshop;

import me.noynto.eosa.shared.WorkshopBookingId;
import me.noynto.eosa.shared.WorkshopSessionId;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public interface WorkshopBookingProvider {

    /**
     * Identifiants des réservations, de la plus ancienne à la plus récente. Chaque filtre est ignoré
     * s'il est null ; l'e-mail est comparé sans tenir compte de la casse.
     */
    Stream<WorkshopBookingId> readIds(WorkshopSessionId sessionId, Set<WorkshopBookingState> states,
                                      String email, String token);

    Optional<WorkshopBooking> read(WorkshopBookingId workshopBookingId);

    WorkshopBooking write(WorkshopBooking workshopBooking);

}
