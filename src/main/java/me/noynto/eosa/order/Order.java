package me.noynto.eosa.order;

import me.noynto.eosa.shared.CheckoutSessionId;
import me.noynto.eosa.shared.OrderId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Commande de bijoux payée. Elle est créée à la confirmation du paiement Stripe, jamais avant,
 * et copie ce qui a été acheté.
 */
public class Order {

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
    private Instant createdAt;

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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
