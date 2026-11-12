package me.noynto.eosa.shipping;

public interface ShippingLabelProvider {

    /**
     * Crée l'étiquette chez le transporteur (commande, destinataire, poids) et rend le même objet avec son numéro de
     * suivi, l'adresse du PDF et le statut {@link ShippingLabelStatus#CREATED}. Une étiquette refusée ou un
     * transporteur injoignable lève une exception dont le message est lisible par l'administratrice.
     * <p>
     * La création n'est pas idempotente côté transporteur : l'appeler deux fois crée deux étiquettes.
     */
    ShippingLabel write(ShippingLabel label);

}
