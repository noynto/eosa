package me.noynto.eosa.infrastructure.fetch.mondialrelay.adapter;

import me.noynto.eosa.infrastructure.fetch.mondialrelay.config.MondialRelayConnectProperties;
import me.noynto.eosa.infrastructure.fetch.mondialrelay.config.MondialRelayTrackingProperties;
import me.noynto.eosa.infrastructure.fetch.mondialrelay.resource.MondialRelayShipmentResource;
import me.noynto.eosa.infrastructure.fetch.mondialrelay.resource.MondialRelayTrackingResource;
import me.noynto.eosa.shared.ShippingLabelId;
import me.noynto.eosa.shipping.ShippingCarrier;
import me.noynto.eosa.shipping.ShippingLabel;
import me.noynto.eosa.shipping.ShippingLabelProvider;
import me.noynto.eosa.shipping.ShippingLabelStatus;

import java.io.IOException;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;

/**
 * Étiquettes Mondial Relay : créées par l'API Connect (V2) et relues par le web service de suivi (V1), qui ont chacun
 * leurs identifiants.
 */
public record MondialRelayFetchedShippingLabels(
        MondialRelayShipmentResource shipmentResource,
        MondialRelayConnectProperties properties,
        MondialRelayTrackingResource trackingResource,
        MondialRelayTrackingProperties trackingProperties
) implements ShippingLabelProvider {

    /**
     * Premier événement d'un colis : le transporteur reçoit les données de l'étiquette, le colis n'est pas encore
     * entre ses mains.
     */
    private static final String ANNOUNCEMENT = "RECEPTION DES DONNEES";

    @Override
    public ShippingLabel write(ShippingLabel label) {
        if (!properties.isConfigured()) {
            throw new RuntimeException("Mondial Relay n'est pas configuré : EOSA_CLIENT_MONDIAL_RELAY_V2_* et EOSA_SHIPPING_SENDER_* sont nécessaires.");
        }
        if (label.getWeightGrams() < MondialRelayShipmentResource.MIN_WEIGHT_GRAMS
                || label.getWeightGrams() > MondialRelayShipmentResource.MAX_WEIGHT_GRAMS) {
            throw new RuntimeException("Le poids du colis doit être compris entre "
                    + MondialRelayShipmentResource.MIN_WEIGHT_GRAMS + " g et "
                    + MondialRelayShipmentResource.MAX_WEIGHT_GRAMS + " g.");
        }
        if (label.getLengthCm() <= 0 || label.getWidthCm() <= 0 || label.getDepthCm() <= 0) {
            throw new RuntimeException("Les dimensions du colis sont nécessaires : Mondial Relay refuse une étiquette sans elles.");
        }
        MondialRelayShipmentResource.Created created;
        try {
            created = shipmentResource.create(label);
        } catch (IOException e) {
            throw new RuntimeException("Mondial Relay est injoignable ou sa réponse est illisible : réessayez dans un instant.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("La création de l'étiquette a été interrompue.", e);
        }
        label.setCarrier(ShippingCarrier.MONDIAL_RELAY);
        label.setTrackingNumber(created.shipmentNumber());
        label.setReference(created.labelUrl());
        label.setStatus(ShippingLabelStatus.CREATED);
        return label;
    }

    @Override
    public Optional<ShippingLabel> read(ShippingLabelId labelId) {
        if (!trackingProperties.isConfigured()) {
            throw new RuntimeException("Le suivi Mondial Relay n'est pas configuré : EOSA_CLIENT_MONDIAL_RELAY_V1_* est nécessaire.");
        }
        MondialRelayTrackingResource.Tracking tracking;
        try {
            tracking = trackingResource.track(labelId.value());
        } catch (IOException e) {
            throw new RuntimeException("Le suivi Mondial Relay est injoignable ou sa réponse est illisible.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("La lecture du suivi a été interrompue.", e);
        }
        return toLabel(tracking, labelId.value());
    }

    /**
     * Statut du colis d'après le code de suivi de Mondial Relay : 80 colis enregistré, 81 pris en charge par le
     * réseau (donc déposé), 82 livré. Une anomalie (83) ou tout autre code lève une exception, que l'appelant compte
     * comme un échec pour cette commande.
     */
    static Optional<ShippingLabel> toLabel(MondialRelayTrackingResource.Tracking tracking, String trackingNumber) {
        int stat = tracking.stat();
        ShippingLabelStatus status;
        switch (stat) {
            case MondialRelayTrackingResource.STAT_RECORDED -> status = ShippingLabelStatus.CREATED;
            case MondialRelayTrackingResource.STAT_IN_PROGRESS -> status = ShippingLabelStatus.DEPOSITED;
            case MondialRelayTrackingResource.STAT_DELIVERED -> status = ShippingLabelStatus.DELIVERED;
            case MondialRelayTrackingResource.STAT_UNKNOWN_PARCEL -> {
                return Optional.empty();
            }
            case MondialRelayTrackingResource.STAT_ANOMALY -> throw new RuntimeException(
                    "Mondial Relay signale une anomalie sur le colis " + trackingNumber
                            + (tracking.message().isBlank() ? "" : " : " + tracking.message()) + ".");
            default -> throw new RuntimeException(
                    "Mondial Relay a refusé de lire le suivi du colis " + trackingNumber + " (code " + stat + ").");
        }

        ShippingLabel label = new ShippingLabel();
        label.setCarrier(ShippingCarrier.MONDIAL_RELAY);
        label.setTrackingNumber(trackingNumber);
        label.setStatus(status);
        var events = tracking.events();
        if (status != ShippingLabelStatus.CREATED) {
            // Le dépôt est le premier événement qui n'est pas l'annonce de l'étiquette.
            events.stream()
                    .filter(event -> !normalize(event.label()).equals(ANNOUNCEMENT))
                    .findFirst()
                    .ifPresent(event -> label.setDepositedAt(event.at()));
        }
        if (status == ShippingLabelStatus.DELIVERED && !events.isEmpty()) {
            label.setDeliveredAt(events.getLast().at());
        }
        return Optional.of(label);
    }

    /** Sans accent ni casse, pour comparer des libellés (« RÉCEPTION DES DONNÉES »). */
    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toUpperCase(Locale.ROOT).strip();
    }

}
