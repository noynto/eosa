package me.noynto.eosa.giftcard;

/**
 * État enregistré d'une carte. L'expiration n'est pas un état : elle se déduit de la date de fin de validité
 * (voir {@link GiftCard#isExpired}).
 */
public enum GiftCardState {
    /** Créée, en attente du paiement. */
    PENDING_PAYMENT,
    /** Payée ou offerte : utilisable tant qu'elle n'est pas expirée et que son solde est positif. */
    ACTIVE,
    /** Annulée par l'administration : définitive. */
    VOID
}
