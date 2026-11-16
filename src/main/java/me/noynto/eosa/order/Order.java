package me.noynto.eosa.order;

import me.noynto.eosa.shared.CheckoutSessionId;
import me.noynto.eosa.shared.OrderId;
import me.noynto.eosa.shared.GiftCardId;
import me.noynto.eosa.shared.PromoCodeId;
import me.noynto.eosa.shipping.ShippingLabel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Commande de bijoux payée. Elle est créée à la confirmation du paiement Stripe, jamais avant,
 * et copie ce qui a été acheté.
 */
public class Order {

    /**
     * Délai de fabrication annoncé, en heures ouvrées, à compter du paiement.
     */
    public static final long CRAFTING_HOURS = 48;

    /**
     * Délai d'expédition annoncé, en heures ouvrées, à compter de la fabrication.
     */
    public static final long SHIPPING_HOURS = 24;

    private OrderId id;
    private String number;
    private OrderState state;
    private CheckoutSessionId stripeSessionId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private OrderShippingAddress shippingAddress;
    private List<OrderItem> items = new ArrayList<>();
    private BigDecimal itemsAmount;
    private BigDecimal discountAmount = BigDecimal.ZERO;
    private String promoCode;
    private PromoCodeId promoCodeId;
    private BigDecimal giftCardAmount = BigDecimal.ZERO;
    private GiftCardId giftCardId;
    private BigDecimal shippingAmount;
    private BigDecimal totalAmount;
    private Instant paidAt;
    private boolean handDelivery;
    private ShippingLabel label;
    private Instant craftedAt;
    private Instant shippedAt;
    private Instant deliveredAt;
    private Instant cancelledAt;
    private String cancelReason;
    private Instant shippingSentAt;
    private Instant personalDataPurgedAt;
    private Instant createdAt;
    private Instant confirmationSentAt;
    private Instant notificationSentAt;

    public Order() {
    }

    public OrderId getId() {
        return id;
    }

    public void setId(OrderId id) {
        this.id = id;
    }

    /**
     * Numéro lisible (par exemple EOSA-2026-0001), attribué à la première écriture.
     */
    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public OrderState getState() {
        return state;
    }

    public void setState(OrderState state) {
        this.state = state;
    }

    public CheckoutSessionId getStripeSessionId() {
        return stripeSessionId;
    }

    public void setStripeSessionId(CheckoutSessionId stripeSessionId) {
        this.stripeSessionId = stripeSessionId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public OrderShippingAddress getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(OrderShippingAddress shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public BigDecimal getItemsAmount() {
        return itemsAmount;
    }

    public void setItemsAmount(BigDecimal itemsAmount) {
        this.itemsAmount = itemsAmount;
    }

    /**
     * Remise accordée par un code de promotion, déjà déduite du total payé ; zéro sans code. Le total vaut
     * les bijoux, moins la remise, plus la livraison.
     */
    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    /**
     * Part du total payée par une carte cadeau, déjà déduite du total payé chez Stripe ; zéro sans carte, et jamais
     * en même temps qu'une remise de code de promotion. Le total vaut les bijoux, moins la part de la carte, plus
     * la livraison.
     */
    public BigDecimal getGiftCardAmount() {
        return giftCardAmount;
    }

    public void setGiftCardAmount(BigDecimal giftCardAmount) {
        this.giftCardAmount = giftCardAmount;
    }

    /** La carte cadeau qui a payé une part de la commande, pour la retrouver ; null sans carte. */
    public GiftCardId getGiftCardId() {
        return giftCardId;
    }

    public void setGiftCardId(GiftCardId giftCardId) {
        this.giftCardId = giftCardId;
    }

    /** Le code de promotion utilisé, tel qu'il s'appelait à la commande ; null sans code. */
    public String getPromoCode() {
        return promoCode;
    }

    public void setPromoCode(String promoCode) {
        this.promoCode = promoCode;
    }

    /** Identifiant du code de promotion utilisé, pour compter ses usages ; null sans code. */
    public PromoCodeId getPromoCodeId() {
        return promoCodeId;
    }

    public void setPromoCodeId(PromoCodeId promoCodeId) {
        this.promoCodeId = promoCodeId;
    }

    public BigDecimal getShippingAmount() {
        return shippingAmount;
    }

    public void setShippingAmount(BigDecimal shippingAmount) {
        this.shippingAmount = shippingAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Instant paidAt) {
        this.paidAt = paidAt;
    }

    /** Instant d'envoi de l'e-mail de confirmation à la cliente, null tant qu'il n'est pas parti. */
    public Instant getConfirmationSentAt() {
        return confirmationSentAt;
    }

    public void setConfirmationSentAt(Instant confirmationSentAt) {
        this.confirmationSentAt = confirmationSentAt;
    }

    /** Instant d'envoi de l'e-mail « nouvelle commande » à EOSA, null tant qu'il n'est pas parti. */
    public Instant getNotificationSentAt() {
        return notificationSentAt;
    }

    public void setNotificationSentAt(Instant notificationSentAt) {
        this.notificationSentAt = notificationSentAt;
    }

    /**
     * La commande est remise en main propre : ni transporteur, ni étiquette, ni suivi. Décidé à la fabrication ;
     * elle passe alors de fabriquée à livrée sans être expédiée.
     */
    public boolean isHandDelivery() {
        return handDelivery;
    }

    public void setHandDelivery(boolean handDelivery) {
        this.handDelivery = handDelivery;
    }

    /** Étiquette d'expédition : présente dès que la commande est fabriquée pour être expédiée, jamais avant. */
    public ShippingLabel getLabel() {
        return label;
    }

    public void setLabel(ShippingLabel label) {
        this.label = label;
    }

    /** Instant où la pièce a été déclarée fabriquée, null tant que la commande est à fabriquer. */
    public Instant getCraftedAt() {
        return craftedAt;
    }

    public void setCraftedAt(Instant craftedAt) {
        this.craftedAt = craftedAt;
    }

    /** Instant où le colis a été remis au transporteur, null avant. */
    public Instant getShippedAt() {
        return shippedAt;
    }

    public void setShippedAt(Instant shippedAt) {
        this.shippedAt = shippedAt;
    }

    /** Instant de la livraison, null avant. */
    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(Instant deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    /** Instant de l'annulation, null si la commande n'est pas annulée. */
    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    /** Motif de l'annulation, noté par l'administratrice, null si la commande n'est pas annulée. */
    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    /**
     * Instant où l'adresse de livraison et le téléphone ont été effacés au terme de leur durée de conservation,
     * null tant qu'ils sont gardés.
     */
    public Instant getPersonalDataPurgedAt() {
        return personalDataPurgedAt;
    }

    public void setPersonalDataPurgedAt(Instant personalDataPurgedAt) {
        this.personalDataPurgedAt = personalDataPurgedAt;
    }

    /** Instant d'envoi de l'e-mail d'expédition à la cliente, null tant qu'il n'est pas parti. */
    public Instant getShippingSentAt() {
        return shippingSentAt;
    }

    public void setShippingSentAt(Instant shippingSentAt) {
        this.shippingSentAt = shippingSentAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
