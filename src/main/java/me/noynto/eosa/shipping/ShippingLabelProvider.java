package me.noynto.eosa.shipping;

import me.noynto.eosa.shared.ShippingLabelId;

import java.util.Optional;

public interface ShippingLabelProvider {

    /**
     * Crée l'étiquette chez le transporteur (commande, destinataire, poids) et rend le même objet avec son numéro de
     * suivi, l'adresse du PDF et le statut {@link ShippingLabelStatus#CREATED}. Une étiquette refusée ou un
     * transporteur injoignable lève une exception dont le message est lisible par l'administratrice.
     * <p>
     * La création n'est pas idempotente côté transporteur : l'appeler deux fois crée deux étiquettes.
     */
    ShippingLabel write(ShippingLabel label);

    /**
     * Relit l'étiquette chez le transporteur : son statut (créée, déposée, livrée) et les dates de dépôt et de
     * livraison données par le transporteur. Vide si le transporteur ne connaît pas ce numéro de suivi. Une
     * anomalie signalée sur le colis, un refus ou un transporteur injoignable lèvent une exception.
     */
    Optional<ShippingLabel> read(ShippingLabelId labelId);

}
