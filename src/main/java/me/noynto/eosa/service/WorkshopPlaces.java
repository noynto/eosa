package me.noynto.eosa.service;

import me.noynto.eosa.workshop.WorkshopBooking;
import me.noynto.eosa.workshop.WorkshopBookingState;
import me.noynto.eosa.workshop.WorkshopSession;

import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

/**
 * Les places d'une session, d'après ses réservations : une règle qui croise la session (sa capacité) et les
 * réservations (celles qui occupent une place). Les réservations sont lues par le cas d'usage.
 */
public final class WorkshopPlaces {

    /**
     * Les états de réservation à lire pour compter les places occupées ; {@link WorkshopBooking#occupiesAPlace}
     * écarte ensuite celles dont le délai de confirmation est passé.
     */
    public static final Set<WorkshopBookingState> OCCUPYING_STATES = Set.copyOf(EnumSet.of(
            WorkshopBookingState.PENDING, WorkshopBookingState.CONFIRMED,
            WorkshopBookingState.ATTENDED, WorkshopBookingState.NO_SHOW
    ));

    private WorkshopPlaces() {
    }

    public static int occupied(Collection<WorkshopBooking> bookings, Instant now) {
        return (int) bookings.stream().filter(booking -> booking.occupiesAPlace(now)).count();
    }

    public static int free(WorkshopSession session, Collection<WorkshopBooking> bookings, Instant now) {
        return Math.max(0, session.getCapacity() - occupied(bookings, now));
    }

}
