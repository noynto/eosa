package me.noynto.eosa.giftcard;

import me.noynto.eosa.shared.GiftCardId;

import java.util.Optional;
import java.util.stream.Stream;

public interface GiftCardProvider {

    /**
     * Identifiants des cartes, les plus récentes d'abord. Chaque filtre est ignoré s'il est null : {@code code}
     * (exact, déjà normalisé) et {@code token}.
     */
    Stream<GiftCardId> readIds(String code, String token);

    /** La carte avec son journal, du plus ancien mouvement au plus récent. */
    Optional<GiftCard> read(GiftCardId giftCardId);

    /**
     * Écrit la carte et ajoute les mouvements qui n'ont pas encore d'identifiant (le journal ne se réécrit pas).
     * Attribue les identifiants manquants.
     */
    GiftCard write(GiftCard giftCard);

    void delete(GiftCardId giftCardId);

}
