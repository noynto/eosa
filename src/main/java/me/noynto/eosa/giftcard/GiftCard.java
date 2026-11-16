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

    /**
     * Met de côté une part du solde pour un paiement en cours : le solde baisse tout de suite, et la session de
     * paiement est rattachée à la réservation quand elle existe ({@link GiftCardMovement#setCheckoutSessionId}).
     */
    public GiftCardMovement reserve(BigDecimal amount, Instant now) {
        var reservation = new GiftCardMovement(GiftCardMovementKind.RESERVED, amount, null, now);
        movements.add(reservation);
        return reservation;
    }

    /** Rend une réservation au solde (session expirée, abandonnée ou jamais ouverte). */
    public void release(GiftCardMovement reservation, Instant now) {
        var released = new GiftCardMovement(GiftCardMovementKind.RELEASED, reservation.getAmount(), null, now);
        released.setCheckoutSessionId(reservation.getCheckoutSessionId());
        movements.add(released);
    }

    /**
     * Réservations rattachées à une session de paiement et ni utilisées ni rendues, de la plus ancienne à la plus
     * récente. Une session compte autant de réservations que de fois où elle a été réservée : elle est ouverte
     * tant qu'il y en a plus que d'utilisations et de libérations.
     */
    public List<GiftCardMovement> openReservations() {
        List<GiftCardMovement> open = new ArrayList<>();
        for (GiftCardMovement reservation : movements) {
            String session = reservation.getCheckoutSessionId();
            if (reservation.getKind() != GiftCardMovementKind.RESERVED || session == null) {
                continue;
            }
            long reserved = countOf(GiftCardMovementKind.RESERVED, session);
            long closed = countOf(GiftCardMovementKind.USED, session) + countOf(GiftCardMovementKind.RELEASED, session);
            if (reserved > closed && open.stream().noneMatch(other -> session.equals(other.getCheckoutSessionId()))) {
                open.add(reservation);
            }
        }
        return open;
    }

    /**
     * Enregistre qu'un paiement abouti a utilisé {@code amount} de la carte, une seule fois par session. Le solde
     * avait baissé à la réservation ; si elle manque ou a déjà été rendue (paiement très tardif, ou session que le
     * site n'avait pas réservée), le montant est retiré maintenant. Rend faux si la session était déjà enregistrée.
     */
    public boolean markUsed(String sessionId, BigDecimal amount, Instant now) {
        if (countOf(GiftCardMovementKind.USED, sessionId) > 0) {
            return false;
        }
        boolean reserved = countOf(GiftCardMovementKind.RESERVED, sessionId) > 0;
        boolean released = countOf(GiftCardMovementKind.RELEASED, sessionId) > 0;
        if (!reserved || released) {
            var reservation = reserve(amount, now);
            reservation.setCheckoutSessionId(sessionId);
        }
        var used = new GiftCardMovement(GiftCardMovementKind.USED, amount, null, now);
        used.setCheckoutSessionId(sessionId);
        movements.add(used);
        return true;
    }

    private long countOf(GiftCardMovementKind kind, String sessionId) {
        return movements.stream()
                .filter(movement -> movement.getKind() == kind && sessionId.equals(movement.getCheckoutSessionId()))
                .count();
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
