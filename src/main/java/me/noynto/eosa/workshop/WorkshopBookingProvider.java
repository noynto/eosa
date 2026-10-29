package me.noynto.eosa.workshop;

import me.noynto.eosa.shared.WorkshopSessionId;

public interface WorkshopBookingProvider {

    /**
     * Places occupées sur une session : réservations confirmées (ou déjà passées en présente/absente)
     * et réservations en cours sur Stripe dont l'empreinte n'a pas encore expiré.
     */
    int countOccupiedPlaces(WorkshopSessionId workshopSessionId);

}
