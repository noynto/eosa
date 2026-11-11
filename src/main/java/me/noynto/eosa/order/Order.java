package me.noynto.eosa.order;

import me.noynto.eosa.shared.CheckoutSessionId;
import me.noynto.eosa.shared.OrderId;
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
    private BigDecimal shippingAmount;
    private BigDecimal totalAmount;
    private Instant paidAt;
    private ShippingLabel label;
    private Instant craftedAt;
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

    /** Étiquette d'expédition : présente dès que la commande est fabriquée, jamais avant. */
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
