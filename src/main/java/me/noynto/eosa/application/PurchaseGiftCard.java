package me.noynto.eosa.application;

import me.noynto.eosa.checkout.Checkout;
import me.noynto.eosa.checkout.CheckoutItem;
import me.noynto.eosa.checkout.CheckoutProvider;
import me.noynto.eosa.giftcard.GiftCard;
import me.noynto.eosa.giftcard.GiftCardProvider;
import me.noynto.eosa.giftcard.GiftCardState;
import me.noynto.eosa.service.GiftCardIssuing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Achat d'une carte cadeau : crée la carte en attente de paiement puis la session Stripe, et rend l'adresse de
 * paiement. La session expire au bout d'une demi-heure (le minimum de Stripe, avec une minute de marge) : une session
 * abandonnée se ferme d'elle-même. La carte ne devient utilisable qu'au paiement confirmé ; une carte jamais payée
 * ne coûte rien.
 */
public record PurchaseGiftCard(
        GiftCardProvider giftCardProvider,
        CheckoutProvider checkoutProvider,
        Clock clock,
        int validityDays,
        BigDecimal minimumAmount,
        BigDecimal maximumAmount
) {

    static final Duration SESSION_DURATION = Duration.ofMinutes(31);
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_EMAIL_LENGTH = 200;

    public Result handle(Command command) {
        if (command.amount == null) {
            throw new RuntimeException("Choisissez le montant de la carte.");
        }
        if (command.amount.stripTrailingZeros().scale() > 0) {
            throw new RuntimeException("Le montant est un nombre entier d'euros.");
        }
        BigDecimal amount = command.amount.setScale(2, RoundingMode.UNNECESSARY);
        if (amount.compareTo(minimumAmount) < 0 || amount.compareTo(maximumAmount) > 0) {
            throw new RuntimeException("Le montant doit être compris entre " + minimumAmount.intValue()
                    + " € et " + maximumAmount.intValue() + " €.");
        }
        String firstName = command.firstName == null ? "" : command.firstName.strip();
        if (firstName.isEmpty() || firstName.length() > MAX_NAME_LENGTH) {
            throw new RuntimeException("Votre prénom est requis.");
        }
        String email = command.email == null ? "" : command.email.strip();
        if (email.length() > MAX_EMAIL_LENGTH || !EMAIL.matcher(email).matches()) {
            throw new RuntimeException("Votre adresse e-mail est invalide.");
        }

        var now = clock.instant();
        GiftCard giftCard = new GiftCard();
        giftCard.setCode(GiftCardIssuing.newCode(code -> giftCardProvider.readIds(code, null).findAny().isPresent()));
        giftCard.setToken(GiftCardIssuing.newToken());
        giftCard.setState(GiftCardState.PENDING_PAYMENT);
        giftCard.setInitialAmount(amount);
        giftCard.setBuyerFirstName(firstName);
        giftCard.setBuyerEmail(email);
        // Recalculée au paiement confirmé : la validité compte depuis l'achat.
        giftCard.setExpiresAt(GiftCardIssuing.expiresAt(now, validityDays));
        giftCard.setCreatedAt(now);
        giftCard = giftCardProvider.write(giftCard);

        CheckoutItem item = new CheckoutItem();
        item.setName("Carte cadeau EOSA");
        item.setUnitPrice(amount);
        item.setQuantity(1);
        Checkout checkout = new Checkout();
        checkout.setItems(List.of(item));
        checkout.setGiftCardId(giftCard.getId());
        checkout.setCustomerEmail(email);
        checkout.setExpiresAt(now.plus(SESSION_DURATION));
        try {
            checkout = checkoutProvider.write(checkout);
        } catch (RuntimeException e) {
            // Sans session de paiement, la carte en attente ne servirait à rien.
            giftCardProvider.delete(giftCard.getId());
            throw e;
        }

        giftCard.setStripeSessionId(checkout.getSession().getId().getStripe());
        giftCardProvider.write(giftCard);
        return new Result(checkout.getSession().getUri());
    }

    public record Result(
            URI paymentUri
    ) {
    }

    public record Command(
            BigDecimal amount,
            String firstName,
            String email
    ) {
    }

}
