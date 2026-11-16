package me.noynto.eosa.application;

import me.noynto.eosa.giftcard.GiftCard;
import me.noynto.eosa.giftcard.GiftCardMovement;
import me.noynto.eosa.giftcard.GiftCardMovementKind;
import me.noynto.eosa.giftcard.GiftCardProvider;
import me.noynto.eosa.giftcard.GiftCardState;
import me.noynto.eosa.service.GiftCardIssuing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
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

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int MAX_TEXT_LENGTH = 200;

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

        GiftCard giftCard = new GiftCard();
        giftCard.setCode(GiftCardIssuing.newCode(code -> giftCardProvider.readIds(code, null).findAny().isPresent()));
        giftCard.setToken(GiftCardIssuing.newToken());
        giftCard.setState(GiftCardState.ACTIVE);
        giftCard.setInitialAmount(amount);
        giftCard.setBuyerFirstName(firstName);
        giftCard.setBuyerEmail(email);
        giftCard.setExpiresAt(GiftCardIssuing.expiresAt(now, validityDays));
        giftCard.setCreatedAt(now);
        giftCard.getMovements().add(new GiftCardMovement(GiftCardMovementKind.ISSUED, amount, reason, now));
        return new Result(giftCardProvider.write(giftCard));
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
