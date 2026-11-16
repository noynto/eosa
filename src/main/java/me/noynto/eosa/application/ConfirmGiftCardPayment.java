package me.noynto.eosa.application;

import me.noynto.eosa.checkout.Checkout;
import me.noynto.eosa.checkout.CheckoutProvider;
import me.noynto.eosa.checkout.CheckoutStatus;
import me.noynto.eosa.giftcard.GiftCard;
import me.noynto.eosa.giftcard.GiftCardMovement;
import me.noynto.eosa.giftcard.GiftCardMovementKind;
import me.noynto.eosa.giftcard.GiftCardProvider;
import me.noynto.eosa.giftcard.GiftCardState;
import me.noynto.eosa.mail.MailProvider;
import me.noynto.eosa.service.GiftCardIssuing;
import me.noynto.eosa.service.GiftCardMails;
import me.noynto.eosa.shared.CheckoutSessionId;

import java.net.URI;
import java.time.Clock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Au retour de la personne qui a acheté une carte : interroge Stripe et, si le paiement est confirmé, active la
 * carte (premier mouvement du journal, validité comptée depuis maintenant) et envoie l'e-mail « Votre carte cadeau ».
 * Idempotent : recharger la page ne crée rien de plus et n'envoie pas deux e-mails.
 */
public record ConfirmGiftCardPayment(
        GiftCardProvider giftCardProvider,
        CheckoutProvider checkoutProvider,
        MailProvider mailProvider,
        Clock clock,
        int validityDays,
        URI publicBaseUrl
) {

    private static final Logger LOGGER = Logger.getLogger(ConfirmGiftCardPayment.class.getName());

    public Result handle(Command command) {
        if (command.checkoutSessionId == null || command.checkoutSessionId.getStripe() == null
                || command.checkoutSessionId.getStripe().isBlank()) {
            throw new RuntimeException("L'identifiant de session d'achat est nécessaire.");
        }

        Checkout checkout = checkoutProvider.readIds(command.checkoutSessionId, null, null)
                .findFirst()
                .flatMap(checkoutProvider::read)
                .orElseThrow(() -> new RuntimeException("Session Stripe introuvable."));
        if (checkout.getGiftCardId() == null) {
            throw new RuntimeException("Cette session de paiement n'achète pas une carte cadeau.");
        }
        GiftCard giftCard = giftCardProvider.read(checkout.getGiftCardId())
                .orElseThrow(() -> new RuntimeException("Carte cadeau introuvable."));
        if (!command.checkoutSessionId.getStripe().equals(giftCard.getStripeSessionId())) {
            throw new RuntimeException("Cette session de paiement n'est pas celle de la carte.");
        }
        if (checkout.getStatus() != CheckoutStatus.COMPLETED) {
            return new Result(giftCard, false);
        }
        if (checkout.getTotalAmount() == null || checkout.getTotalAmount().compareTo(giftCard.getInitialAmount()) != 0) {
            throw new RuntimeException("Le montant payé ne correspond pas à la carte.");
        }

        var now = clock.instant();
        if (giftCard.getState() == GiftCardState.PENDING_PAYMENT) {
            giftCard.setState(GiftCardState.ACTIVE);
            giftCard.setExpiresAt(GiftCardIssuing.expiresAt(now, validityDays));
            giftCard.getMovements().add(new GiftCardMovement(
                    GiftCardMovementKind.ISSUED, giftCard.getInitialAmount(), "Achat en ligne", now));
            giftCard = giftCardProvider.write(giftCard);
        }
        if (giftCard.getState() == GiftCardState.ACTIVE && giftCard.getEmailSentAt() == null) {
            try {
                mailProvider.send(GiftCardMails.delivery(giftCard, publicBaseUrl));
                giftCard.setEmailSentAt(now);
                giftCard = giftCardProvider.write(giftCard);
            } catch (RuntimeException e) {
                // Un e-mail qui ne part pas ne bloque pas la personne ; il reste sans date d'envoi.
                LOGGER.log(Level.WARNING, "L'e-mail de la carte cadeau " + giftCard.getId().value() + " n'est pas parti.", e);
            }
        }
        return new Result(giftCard, true);
    }

    /**
     * @param paid faux tant que Stripe n'a pas confirmé le paiement (la carte n'est alors pas activée)
     */
    public record Result(
            GiftCard giftCard,
            boolean paid
    ) {
    }

    public record Command(
            CheckoutSessionId checkoutSessionId
    ) {
    }

}
