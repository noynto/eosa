package me.noynto.eosa.infrastructure.fetch.mondialrelay.adapter;

import me.noynto.eosa.infrastructure.fetch.mondialrelay.config.MondialRelayConnectProperties;
import me.noynto.eosa.infrastructure.fetch.mondialrelay.resource.MondialRelayShipmentResource;
import me.noynto.eosa.shipping.ShippingCarrier;
import me.noynto.eosa.shipping.ShippingLabel;
import me.noynto.eosa.shipping.ShippingLabelProvider;
import me.noynto.eosa.shipping.ShippingLabelStatus;

import java.io.IOException;

public record MondialRelayFetchedShippingLabels(
        MondialRelayShipmentResource shipmentResource,
        MondialRelayConnectProperties properties
) implements ShippingLabelProvider {

    @Override
    public ShippingLabel write(ShippingLabel label) {
        if (!properties.isConfigured()) {
            throw new RuntimeException("Mondial Relay n'est pas configuré : EOSA_MONDIAL_RELAY_* et EOSA_SHIPPING_SENDER_* sont nécessaires.");
        }
        if (label.getWeightGrams() < MondialRelayShipmentResource.MIN_WEIGHT_GRAMS
                || label.getWeightGrams() > MondialRelayShipmentResource.MAX_WEIGHT_GRAMS) {
            throw new RuntimeException("Le poids du colis doit être compris entre "
                    + MondialRelayShipmentResource.MIN_WEIGHT_GRAMS + " g et "
                    + MondialRelayShipmentResource.MAX_WEIGHT_GRAMS + " g.");
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

}
