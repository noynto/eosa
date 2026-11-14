package me.noynto.eosa.promo;

import me.noynto.eosa.shared.PromoCodeId;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Code de promotion. La valeur est un pourcentage (1 à 100) ou un montant en euros selon le type.
 * {@code startsAt} est inclusif et {@code endsAt} exclusif ; tout ce qui est facultatif vaut {@code null} quand
 * la limite n'est pas posée.
 */
public class PromoCode {
    private PromoCodeId id;
    private String code;
    private PromoCodeType type;
    private BigDecimal value;
    private Instant startsAt;
    private Instant endsAt;
    private Integer maxUses;
    private BigDecimal minimumAmount;
    private boolean active = true;
    private Instant createdAt;

    public PromoCode() {
    }

    public PromoCodeId getId() {
        return id;
    }

    public void setId(PromoCodeId id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public PromoCodeType getType() {
        return type;
    }

    public void setType(PromoCodeType type) {
        this.type = type;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(Instant startsAt) {
        this.startsAt = startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(Instant endsAt) {
        this.endsAt = endsAt;
    }

    public Integer getMaxUses() {
        return maxUses;
    }

    public void setMaxUses(Integer maxUses) {
        this.maxUses = maxUses;
    }

    public BigDecimal getMinimumAmount() {
        return minimumAmount;
    }

    public void setMinimumAmount(BigDecimal minimumAmount) {
        this.minimumAmount = minimumAmount;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
