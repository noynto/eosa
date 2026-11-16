package me.noynto.eosa.giftcard;

import me.noynto.eosa.shared.GiftCardId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Carte cadeau : un montant à dépenser dans la boutique. Le solde n'est jamais stocké : c'est la somme du
 * journal des mouvements, qui s'écrit avec la carte. {@code expiresAt} est exclusif.
 */
public class GiftCard {
    private GiftCardId id;
    private String code;
    private String token;
    private GiftCardState state;
    private BigDecimal initialAmount;
    private String buyerFirstName;
    private String buyerEmail;
    private Instant emailSentAt;
    private Instant expiresAt;
    private String stripeSessionId;
    private Instant createdAt;
    private List<GiftCardMovement> movements = new ArrayList<>();

    public GiftCard() {
    }

    /** Solde actuel : ce que le journal ajoute moins ce qu'il retire (une utilisation ne change pas le solde). */
    public BigDecimal balance() {
        BigDecimal balance = BigDecimal.ZERO;
        for (GiftCardMovement movement : movements) {
            switch (movement.getKind()) {
                case ISSUED, RELEASED, RESTORED -> balance = balance.add(movement.getAmount());
                case RESERVED, VOIDED -> balance = balance.subtract(movement.getAmount());
                case USED -> {
                }
            }
        }
        return balance;
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && !now.isBefore(expiresAt);
    }

    /** Utilisable : active, non expirée, solde positif. */
    public boolean isUsable(Instant now) {
        return state == GiftCardState.ACTIVE && !isExpired(now) && balance().signum() > 0;
    }

    public GiftCardId getId() {
        return id;
    }

    public void setId(GiftCardId id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public GiftCardState getState() {
        return state;
    }

    public void setState(GiftCardState state) {
        this.state = state;
    }

    public BigDecimal getInitialAmount() {
        return initialAmount;
    }

    public void setInitialAmount(BigDecimal initialAmount) {
        this.initialAmount = initialAmount;
    }

    public String getBuyerFirstName() {
        return buyerFirstName;
    }

    public void setBuyerFirstName(String buyerFirstName) {
        this.buyerFirstName = buyerFirstName;
    }

    public String getBuyerEmail() {
        return buyerEmail;
    }

    public void setBuyerEmail(String buyerEmail) {
        this.buyerEmail = buyerEmail;
    }

    public Instant getEmailSentAt() {
        return emailSentAt;
    }

    public void setEmailSentAt(Instant emailSentAt) {
        this.emailSentAt = emailSentAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getStripeSessionId() {
        return stripeSessionId;
    }

    public void setStripeSessionId(String stripeSessionId) {
        this.stripeSessionId = stripeSessionId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<GiftCardMovement> getMovements() {
        return movements;
    }

    public void setMovements(List<GiftCardMovement> movements) {
        this.movements = movements;
    }
}
