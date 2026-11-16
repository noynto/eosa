package me.noynto.eosa.giftcard;

import me.noynto.eosa.shared.GiftCardMovementId;

import java.math.BigDecimal;
import java.time.Instant;

/** Une ligne du journal d'une carte. Le montant est toujours positif ; le type donne le sens. */
public class GiftCardMovement {
    private GiftCardMovementId id;
    private GiftCardMovementKind kind;
    private BigDecimal amount;
    private String checkoutSessionId;
    private String reason;
    private Instant createdAt;

    public GiftCardMovement() {
    }

    public GiftCardMovement(GiftCardMovementKind kind, BigDecimal amount, String reason, Instant createdAt) {
        this.kind = kind;
        this.amount = amount;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public GiftCardMovementId getId() {
        return id;
    }

    public void setId(GiftCardMovementId id) {
        this.id = id;
    }

    public GiftCardMovementKind getKind() {
        return kind;
    }

    public void setKind(GiftCardMovementKind kind) {
        this.kind = kind;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCheckoutSessionId() {
        return checkoutSessionId;
    }

    public void setCheckoutSessionId(String checkoutSessionId) {
        this.checkoutSessionId = checkoutSessionId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
