package me.noynto.eosa.promo;

import me.noynto.eosa.shared.PromoCodeId;

import java.util.Optional;
import java.util.stream.Stream;

public interface PromoCodeProvider {

    /**
     * Identifiants des codes, les plus récents d'abord. Avec {@code code}, seul le code de ce nom
     * (déjà normalisé) est rendu ; {@code null} ne filtre rien.
     */
    Stream<PromoCodeId> readIds(String code);

    Optional<PromoCode> read(PromoCodeId promoCodeId);

    PromoCode write(PromoCode promoCode);

    void delete(PromoCodeId promoCodeId);

}
