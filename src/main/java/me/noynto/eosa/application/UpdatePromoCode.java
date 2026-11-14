package me.noynto.eosa.application;

import me.noynto.eosa.promo.PromoCode;
import me.noynto.eosa.promo.PromoCodeProvider;
import me.noynto.eosa.promo.PromoCodeType;
import me.noynto.eosa.service.BusinessTime;
import me.noynto.eosa.shared.PromoCodeId;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Modifie les règles d'un code : le code lui-même ne change jamais. Les commandes déjà passées gardent la remise
 * qu'elles ont reçue.
 */
public record UpdatePromoCode(
        PromoCodeProvider promoCodeProvider
) {

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("10000");

    public Result handle(Command command) {
        if (command.promoCodeId == null || command.promoCodeId.value() == null) {
            throw new RuntimeException("L'identifiant du code est nécessaire.");
        }
        if (command.type == null) {
            throw new RuntimeException("Le type de remise est requis.");
        }
        if (command.value == null || command.value.signum() <= 0) {
            throw new RuntimeException("La valeur de la remise est requise.");
        }
        BigDecimal value;
        if (command.type == PromoCodeType.PERCENT) {
            if (command.value.stripTrailingZeros().scale() > 0 || command.value.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new RuntimeException("Le pourcentage est un nombre entier de 1 à 100.");
            }
            value = command.value.setScale(2, RoundingMode.UNNECESSARY);
        } else {
            if (command.value.compareTo(MAX_AMOUNT) > 0) {
                throw new RuntimeException("Le montant de la remise ne peut pas dépasser 10 000 €.");
            }
            value = command.value.setScale(2, RoundingMode.HALF_UP);
        }
        if (command.startsOn != null && command.endsOn != null && command.endsOn.isBefore(command.startsOn)) {
            throw new RuntimeException("La date de fin précède la date de début.");
        }
        if (command.maxUses != null && command.maxUses < 1) {
            throw new RuntimeException("Le nombre d'usages doit être d'au moins 1.");
        }
        BigDecimal minimumAmount = null;
        if (command.minimumAmount != null) {
            if (command.minimumAmount.signum() <= 0 || command.minimumAmount.compareTo(MAX_AMOUNT) > 0) {
                throw new RuntimeException("Le montant minimum doit être un montant positif en euros.");
            }
            minimumAmount = command.minimumAmount.setScale(2, RoundingMode.HALF_UP);
        }

        PromoCode promoCode = promoCodeProvider.read(command.promoCodeId)
                .orElseThrow(() -> new RuntimeException("Le code " + command.promoCodeId.value() + " n'existe pas."));

        promoCode.setType(command.type);
        promoCode.setValue(value);
        promoCode.setStartsAt(command.startsOn == null ? null : command.startsOn.atStartOfDay(BusinessTime.ZONE).toInstant());
        promoCode.setEndsAt(command.endsOn == null ? null : command.endsOn.plusDays(1).atStartOfDay(BusinessTime.ZONE).toInstant());
        promoCode.setMaxUses(command.maxUses);
        promoCode.setMinimumAmount(minimumAmount);
        return new Result(promoCodeProvider.write(promoCode));
    }

    public record Result(
            PromoCode promoCode
    ) {
    }

    public record Command(
            PromoCodeId promoCodeId,
            PromoCodeType type,
            BigDecimal value,
            LocalDate startsOn,
            LocalDate endsOn,
            Integer maxUses,
            BigDecimal minimumAmount
    ) {
    }

}
