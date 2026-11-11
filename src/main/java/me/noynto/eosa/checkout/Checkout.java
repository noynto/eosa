package me.noynto.eosa.checkout;

import me.noynto.eosa.shared.CartId;
import me.noynto.eosa.shared.CheckoutId;

import java.math.BigDecimal;
import java.util.List;

public class Checkout {
    private CheckoutId id;
    private CheckoutSession session;
    private List<CheckoutItem> items;
    private CheckoutStatus status;
    private CartId cartId;
    private BigDecimal shippingAmount;
    private BigDecimal totalAmount;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private CheckoutShippingAddress shippingAddress;

    public Checkout() {
    }

    /**
     * Total payé chez Stripe, livraison comprise. Null tant qu'il n'a pas été lu chez Stripe.
     */
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
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

    public CheckoutShippingAddress getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(CheckoutShippingAddress shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public CheckoutId getId() {
        return id;
    }

    public void setId(CheckoutId id) {
        this.id = id;
    }

    public List<CheckoutItem> getItems() {
        return items;
    }

    public void setItems(List<CheckoutItem> items) {
        this.items = items;
    }

    public CheckoutStatus getStatus() {
        return status;
    }

    public void setStatus(CheckoutStatus status) {
        this.status = status;
    }

    public CheckoutSession getSession() {
        return session;
    }

    public void setSession(CheckoutSession session) {
        this.session = session;
    }

    public CartId getCartId() {
        return cartId;
    }

    public void setCartId(CartId cartId) {
        this.cartId = cartId;
    }

    public BigDecimal getShippingAmount() {
        return shippingAmount;
    }

    public void setShippingAmount(BigDecimal shippingAmount) {
        this.shippingAmount = shippingAmount;
    }
}
