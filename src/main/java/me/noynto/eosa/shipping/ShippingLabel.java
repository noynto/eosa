package me.noynto.eosa.shipping;

import java.time.Instant;

/**
 * Étiquette d'expédition d'une commande. Avant son écriture elle porte ce qu'il faut pour la créer (commande,
 * destinataire, poids) ; après, le numéro de suivi et l'adresse du PDF à imprimer. Seuls le transporteur, le poids,
 * le numéro de suivi, la référence et le statut sont conservés avec la commande.
 */
public class ShippingLabel {

    private ShippingCarrier carrier;
    private String orderNumber;
    private ShippingRecipient recipient;
    private int weightGrams;
    private String trackingNumber;
    private String reference;
    private ShippingLabelStatus status;
    private Instant createdAt;

    public ShippingCarrier getCarrier() {
        return carrier;
    }

    public void setCarrier(ShippingCarrier carrier) {
        this.carrier = carrier;
    }

    /** Numéro de la commande, imprimé sur l'étiquette comme référence. */
    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    /** Destinataire, nul pour une étiquette relue depuis la commande. */
    public ShippingRecipient getRecipient() {
        return recipient;
    }

    public void setRecipient(ShippingRecipient recipient) {
        this.recipient = recipient;
    }

    public int getWeightGrams() {
        return weightGrams;
    }

    public void setWeightGrams(int weightGrams) {
        this.weightGrams = weightGrams;
    }

    /** Numéro de suivi du colis chez le transporteur. */
    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    /** Adresse du PDF de l'étiquette chez le transporteur. */
    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public ShippingLabelStatus getStatus() {
        return status;
    }

    public void setStatus(ShippingLabelStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

}
