package me.noynto.eosa.checkout;

import me.noynto.eosa.shared.CartId;
import me.noynto.eosa.shared.CheckoutId;
import me.noynto.eosa.shared.PromoCodeId;

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
    private BigDecimal discountAmount;
    private PromoCodeId promoCodeId;
    private String promoCode;
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

    /**
     * Remise d'un code de promotion, déduite des bijoux. Null ou zéro sans code. Chez Stripe, elle est portée
     * par un coupon ; à la lecture, c'est la remise réellement appliquée à la session.
     */
    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public PromoCodeId getPromoCodeId() {
        return promoCodeId;
    }

    public void setPromoCodeId(PromoCodeId promoCodeId) {
        this.promoCodeId = promoCodeId;
    }

    public String getPromoCode() {
        return promoCode;
    }

    public void setPromoCode(String promoCode) {
        this.promoCode = promoCode;
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
