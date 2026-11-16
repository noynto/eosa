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
import me.noynto.eosa.shared.GiftCardId;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Rattrape les cartes cadeau, comme {@link SettleOrders} le fait pour les commandes :
 * <ul>
 *   <li>une carte en attente de paiement dont la session Stripe est payée est activée (la personne n'est pas
 *   revenue sur le site) ;</li>
 *   <li>une carte en attente de plus de 24 h dont la session n'est pas payée est supprimée : la session expire en
 *   moins d'une heure, elle ne sera jamais payée. Une session que Stripe ne rend pas ne supprime rien (une panne ne
 *   doit pas faire perdre une carte payée) ;</li>
 *   <li>l'e-mail d'une carte active qui n'est pas parti est renvoyé, y compris celui d'une carte offerte par
 *   l'administration.</li>
 * </ul>
 */
public record SettleGiftCards(
        GiftCardProvider giftCardProvider,
        CheckoutProvider checkoutProvider,
        MailProvider mailProvider,
        Clock clock,
        int validityDays,
        URI publicBaseUrl
) {

    /** Âge à partir duquel une carte jamais payée est supprimée. */
    static final Duration UNPAID_RETENTION = Duration.ofHours(24);

    /** Laisse le temps à l'envoi immédiat d'avoir lieu, pour ne pas envoyer deux fois le même e-mail. */
    static final Duration EMAIL_GRACE_PERIOD = Duration.ofMinutes(5);

    /** Au-delà, on cesse de renvoyer l'e-mail : la carte reste lisible dans l'administration. */
    static final Duration EMAIL_RETRY_WINDOW = Duration.ofDays(7);

    private static final Logger LOGGER = Logger.getLogger(SettleGiftCards.class.getName());

    public Result handle() {
        Instant now = clock.instant();
        List<GiftCardId> ids = giftCardProvider.readIds(null, null).toList();

        int activated = 0;
        int emailsSent = 0;
        int deleted = 0;
        int failed = 0;
        for (GiftCardId id : ids) {
            try {
                GiftCard giftCard = giftCardProvider.read(id).orElseThrow();
                boolean justActivated = false;
                if (giftCard.getState() == GiftCardState.PENDING_PAYMENT) {
                    var outcome = settlePending(giftCard, now);
                    switch (outcome) {
                        case ACTIVATED -> {
                            activated++;
                            justActivated = true;
                            giftCard = giftCardProvider.read(id).orElseThrow();
                        }
                        case DELETED -> {
                            deleted++;
                            continue;
                        }
                        case WAITING -> {
                            continue;
                        }
                    }
                }
                // Une carte que ce passage vient d'activer part tout de suite : personne d'autre ne l'enverra.
                if (justActivated ? giftCard.getEmailSentAt() == null : needsEmail(giftCard, now)) {
                    mailProvider.send(GiftCardMails.delivery(giftCard, publicBaseUrl));
                    giftCard.setEmailSentAt(now);
                    giftCardProvider.write(giftCard);
                    emailsSent++;
                }
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING, "La carte cadeau " + id.value() + " n'a pas pu être rattrapée.", e);
                failed++;
            }
        }
        return new Result(activated, emailsSent, deleted, failed);
    }

    private enum Outcome {ACTIVATED, DELETED, WAITING}

    private Outcome settlePending(GiftCard giftCard, Instant now) {
        boolean old = giftCard.getCreatedAt().isBefore(now.minus(UNPAID_RETENTION));
        if (giftCard.getStripeSessionId() == null) {
            // Jamais arrivée jusqu'à Stripe : rien à attendre.
            if (old) {
                giftCardProvider.delete(giftCard.getId());
                return Outcome.DELETED;
            }
            return Outcome.WAITING;
        }
        var sessionId = new CheckoutSessionId();
        sessionId.setStripe(giftCard.getStripeSessionId());
        Checkout checkout = checkoutProvider.readIds(sessionId, null, null)
                .findFirst()
                .flatMap(checkoutProvider::read)
                .orElse(null);
        if (checkout == null) {
            // Stripe ne rend pas la session (panne ?) : on ne supprime rien, on réessaiera.
            throw new RuntimeException("Session Stripe introuvable : " + giftCard.getStripeSessionId());
        }
        if (checkout.getStatus() == CheckoutStatus.COMPLETED) {
            if (!giftCard.getId().equals(checkout.getGiftCardId())
                    || checkout.getTotalAmount() == null
                    || checkout.getTotalAmount().compareTo(giftCard.getInitialAmount()) != 0) {
                throw new RuntimeException("La session payée ne correspond pas à la carte " + giftCard.getId().value() + ".");
            }
            giftCard.setState(GiftCardState.ACTIVE);
            giftCard.setExpiresAt(GiftCardIssuing.expiresAt(now, validityDays));
            giftCard.getMovements().add(new GiftCardMovement(
                    GiftCardMovementKind.ISSUED, giftCard.getInitialAmount(), "Achat en ligne", now));
            giftCardProvider.write(giftCard);
            return Outcome.ACTIVATED;
        }
        if (old) {
            giftCardProvider.delete(giftCard.getId());
            return Outcome.DELETED;
        }
        return Outcome.WAITING;
    }

    /** Active, sans e-mail parti, émise depuis plus de 5 minutes et pas depuis plus de 7 jours. */
    private boolean needsEmail(GiftCard giftCard, Instant now) {
        if (giftCard.getState() != GiftCardState.ACTIVE || giftCard.getEmailSentAt() != null
                || giftCard.getBuyerEmail() == null) {
            return false;
        }
        Instant issuedAt = giftCard.getMovements().stream()
                .filter(movement -> movement.getKind() == GiftCardMovementKind.ISSUED)
                .map(GiftCardMovement::getCreatedAt)
                .findFirst()
                .orElse(giftCard.getCreatedAt());
        return issuedAt.isBefore(now.minus(EMAIL_GRACE_PERIOD)) && issuedAt.isAfter(now.minus(EMAIL_RETRY_WINDOW));
    }

    /**
     * @param activated  cartes payées activées par ce passage
     * @param emailsSent e-mails de carte partis pendant ce passage
     * @param deleted    cartes jamais payées supprimées
     * @param failed     cartes non rattrapées et e-mails non partis (repris au prochain passage)
     */
    public record Result(
            int activated,
            int emailsSent,
            int deleted,
            int failed
    ) {
    }

}
