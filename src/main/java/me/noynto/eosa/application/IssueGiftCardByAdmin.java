package me.noynto.eosa.application;

import me.noynto.eosa.giftcard.GiftCard;
import me.noynto.eosa.giftcard.GiftCardMovement;
import me.noynto.eosa.giftcard.GiftCardMovementKind;
import me.noynto.eosa.giftcard.GiftCardProvider;
import me.noynto.eosa.giftcard.GiftCardState;
import me.noynto.eosa.service.BusinessTime;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * Émet une carte offerte sans paiement (geste commercial, lot d'un concours) : elle est active tout de suite,
 * avec son montant initial dans le journal et le motif de l'émission. La validité compte en jours calendaires
 * (fuseau de Paris) : le dernier jour est inclus.
 */
public record IssueGiftCardByAdmin(
        GiftCardProvider giftCardProvider,
        Clock clock,
        int validityDays
) {

    /** Sans caractères ambigus (0/O, 1/I/L). */
    static final String CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    static final int CODE_LENGTH = 12;
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int MAX_TEXT_LENGTH = 200;
    private static final SecureRandom RANDOM = new SecureRandom();

    public Result handle(Command command) {
        if (command.amount == null || command.amount.signum() <= 0 || command.amount.compareTo(MAX_AMOUNT) > 0) {
            throw new RuntimeException("Le montant doit être positif et d'au plus 1 000 €.");
        }
        BigDecimal amount = command.amount.setScale(2, RoundingMode.HALF_UP);
        String email = strip(command.recipientEmail);
        if (email == null || email.length() > MAX_TEXT_LENGTH || !EMAIL.matcher(email).matches()) {
            throw new RuntimeException("L'adresse e-mail de la personne qui reçoit la carte est invalide.");
        }
        String firstName = strip(command.recipientFirstName);
        if (firstName != null && firstName.length() > MAX_TEXT_LENGTH) {
            throw new RuntimeException("Le prénom est trop long.");
        }
        String reason = strip(command.reason);
        if (reason == null || reason.length() > MAX_TEXT_LENGTH) {
            throw new RuntimeException("Le motif de l'émission est requis.");
        }

        var now = clock.instant();
        var firstDayAfterValidity = now.atZone(BusinessTime.ZONE).toLocalDate()
                .plusDays(validityDays + 1L).atStartOfDay(BusinessTime.ZONE).toInstant();

        GiftCard giftCard = new GiftCard();
        giftCard.setCode(newCode());
        giftCard.setToken(newToken());
        giftCard.setState(GiftCardState.ACTIVE);
        giftCard.setInitialAmount(amount);
        giftCard.setBuyerFirstName(firstName);
        giftCard.setBuyerEmail(email);
        giftCard.setExpiresAt(firstDayAfterValidity);
        giftCard.setCreatedAt(now);
        giftCard.getMovements().add(new GiftCardMovement(GiftCardMovementKind.ISSUED, amount, reason, now));
        return new Result(giftCardProvider.write(giftCard));
    }

    private String newCode() {
        for (int attempt = 0; attempt < 5; attempt++) {
            var code = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                code.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            }
            if (giftCardProvider.readIds(code.toString(), null).findAny().isEmpty()) {
                return code.toString();
            }
        }
        throw new RuntimeException("Impossible de générer un code unique, réessayez.");
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String strip(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }

    public record Result(
            GiftCard giftCard
    ) {
    }

    public record Command(
            BigDecimal amount,
            String recipientFirstName,
            String recipientEmail,
            String reason
    ) {
    }

}
