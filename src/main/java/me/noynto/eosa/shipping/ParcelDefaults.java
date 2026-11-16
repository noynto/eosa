package me.noynto.eosa.shipping;

/**
 * Poids et dimensions du colis proposés à la fabrication d'une commande. Aucun bijou n'a de poids ni de dimensions
 * dans le modèle : l'administratrice les confirme ou les corrige à chaque fabrication.
 *
 * @param weightGrams poids en grammes
 * @param lengthCm    longueur en centimètres
 * @param widthCm     largeur en centimètres
 * @param depthCm     profondeur en centimètres
 */
public record ParcelDefaults(
        int weightGrams,
        int lengthCm,
        int widthCm,
        int depthCm
) {
}
