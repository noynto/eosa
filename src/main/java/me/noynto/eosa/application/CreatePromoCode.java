package me.noynto.eosa.application;

import me.noynto.eosa.promo.PromoCode;
import me.noynto.eosa.promo.PromoCodeProvider;
import me.noynto.eosa.promo.PromoCodeType;
import me.noynto.eosa.service.BusinessTime;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Crée un code de promotion. Le code est normalisé (majuscules sans accent ni espace) et unique ; le pourcentage
 * va de 1 à 100 et le montant fixe est en euros. Le dernier jour de validité est inclus.
 */
public record CreatePromoCode(
        PromoCodeProvider promoCodeProvider,
        Clock clock
) {

    private static final Pattern VALID_CODE = Pattern.compile("[A-Z0-9-]{3,30}");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("10000");

    public Result handle(Command command) {
        String code = normalize(command.code);
        if (code.isEmpty()) {
            throw new RuntimeException("Le code est requis.");
        }
        if (!VALID_CODE.matcher(code).matches()) {
            throw new RuntimeException("Le code compte de 3 à 30 caractères : lettres, chiffres et tirets.");
        }
        if (command.type == null) {
            throw new RuntimeException("Le type de remise est requis.");
        }
        BigDecimal value = checkedValue(command.type, command.value);
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
        if (promoCodeProvider.readIds(code).findAny().isPresent()) {
            throw new RuntimeException("Le code " + code + " existe déjà.");
        }

        PromoCode promoCode = new PromoCode();
        promoCode.setCode(code);
        promoCode.setType(command.type);
        promoCode.setValue(value);
        promoCode.setStartsAt(command.startsOn == null ? null : command.startsOn.atStartOfDay(BusinessTime.ZONE).toInstant());
        promoCode.setEndsAt(command.endsOn == null ? null : command.endsOn.plusDays(1).atStartOfDay(BusinessTime.ZONE).toInstant());
        promoCode.setMaxUses(command.maxUses);
        promoCode.setMinimumAmount(minimumAmount);
        promoCode.setActive(true);
        promoCode.setCreatedAt(clock.instant());
        return new Result(promoCodeProvider.write(promoCode));
    }

    private static BigDecimal checkedValue(PromoCodeType type, BigDecimal value) {
        if (value == null || value.signum() <= 0) {
            throw new RuntimeException("La valeur de la remise est requise.");
        }
        if (type == PromoCodeType.PERCENT) {
            if (value.stripTrailingZeros().scale() > 0 || value.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new RuntimeException("Le pourcentage est un nombre entier de 1 à 100.");
            }
            return value.setScale(2, RoundingMode.UNNECESSARY);
        }
        if (value.compareTo(MAX_AMOUNT) > 0) {
            throw new RuntimeException("Le montant de la remise ne peut pas dépasser 10 000 €.");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    /** Majuscules, sans accent, sans espace : « été 10 » devient « ETE10 ». */
    static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("\\s+", "");
    }

    public record Result(
            PromoCode promoCode
    ) {
    }

    public record Command(
            String code,
            PromoCodeType type,
            BigDecimal value,
            LocalDate startsOn,
            LocalDate endsOn,
            Integer maxUses,
            BigDecimal minimumAmount
    ) {
    }

}
