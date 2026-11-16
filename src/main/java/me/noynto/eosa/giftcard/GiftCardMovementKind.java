package me.noynto.eosa.giftcard;

/** Nature d'une ligne du journal d'une carte. */
public enum GiftCardMovementKind {
    /** Montant initial de la carte (ajoute au solde). */
    ISSUED,
    /** Solde mis de côté par un paiement en cours (retire du solde). */
    RESERVED,
    /** Réservation confirmée par un paiement abouti : le solde avait déjà baissé à la réservation. */
    USED,
    /** Réservation rendue, session expirée ou abandonnée (ajoute au solde). */
    RELEASED,
    /** Solde restant retiré par l'annulation de la carte (retire du solde). */
    VOIDED,
    /** Montant rendu à la main par l'administration (ajoute au solde). */
    RESTORED
}
