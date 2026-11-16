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
    private int lengthCm;
    private int widthCm;
    private int depthCm;
    private String trackingNumber;
    private String reference;
    private ShippingLabelStatus status;
    private Instant depositedAt;
    private Instant deliveredAt;
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

    /** Longueur du colis en centimètres, saisie à la fabrication. */
    public int getLengthCm() {
        return lengthCm;
    }

    public void setLengthCm(int lengthCm) {
        this.lengthCm = lengthCm;
    }

    /** Largeur du colis en centimètres, saisie à la fabrication. */
    public int getWidthCm() {
        return widthCm;
    }

    public void setWidthCm(int widthCm) {
        this.widthCm = widthCm;
    }

    /** Profondeur du colis en centimètres, saisie à la fabrication. */
    public int getDepthCm() {
        return depthCm;
    }

    public void setDepthCm(int depthCm) {
        this.depthCm = depthCm;
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

    /** Instant où le transporteur a pris le colis en charge, d'après son suivi. Renseigné par la lecture seulement. */
    public Instant getDepositedAt() {
        return depositedAt;
    }

    public void setDepositedAt(Instant depositedAt) {
        this.depositedAt = depositedAt;
    }

    /** Instant de la livraison, d'après le suivi du transporteur. Renseigné par la lecture seulement. */
    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(Instant deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

}
