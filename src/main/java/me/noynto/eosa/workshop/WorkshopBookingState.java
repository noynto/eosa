package me.noynto.eosa.workshop;

public enum WorkshopBookingState {
    /** En attente de confirmation depuis l'e-mail : la place est retenue pendant le délai de confirmation. */
    PENDING,
    CONFIRMED,
    CANCELLED,
    /** Jamais confirmée dans le délai : la place est libérée. */
    EXPIRED,
    ATTENDED,
    NO_SHOW
}
