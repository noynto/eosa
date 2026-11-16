package me.noynto.eosa.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.function.Predicate;

/**
 * Ce que partagent l'émission manuelle et l'achat d'une carte cadeau : le code, le jeton et la fin de validité.
 */
public final class GiftCardIssuing {

    /** Sans caractères ambigus (0/O, 1/I/L). */
    public static final String CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    public static final int CODE_LENGTH = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    private GiftCardIssuing() {
    }

    /** Code aléatoire que {@code taken} ne reconnaît pas encore ; échoue après quelques tirages malchanceux. */
    public static String newCode(Predicate<String> taken) {
        for (int attempt = 0; attempt < 5; attempt++) {
            var code = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                code.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            }
            if (!taken.test(code.toString())) {
                return code.toString();
            }
        }
        throw new RuntimeException("Impossible de générer un code unique, réessayez.");
    }

    /** Jeton de 32 octets aléatoires, qui ouvre la page imprimable de la carte. */
    public static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Fin de validité (exclusive) d'une carte émise à {@code now} : la validité compte en jours calendaires à Paris
     * et le dernier jour est inclus.
     */
    public static Instant expiresAt(Instant now, int validityDays) {
        return now.atZone(BusinessTime.ZONE).toLocalDate()
                .plusDays(validityDays + 1L).atStartOfDay(BusinessTime.ZONE).toInstant();
    }

}
