package me.noynto.eosa.checkout;

import me.noynto.eosa.shared.CheckoutItemId;
import me.noynto.eosa.shared.ImageId;
import me.noynto.eosa.shared.ProductId;

import java.math.BigDecimal;

public class CheckoutItem {
    private CheckoutItemId id;
    private ProductId productId;
    private String name;
    private BigDecimal unitPrice;
    private int quantity;
    private ImageId imageId;

    public CheckoutItem() {
    }

    public CheckoutItemId getId() {
        return id;
    }

    public void setId(CheckoutItemId id) {
        this.id = id;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public ProductId getProductId() {
        return productId;
    }

    public void setProductId(ProductId productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public ImageId getImageId() {
        return imageId;
    }

    public void setImageId(ImageId imageId) {
        this.imageId = imageId;
    }
}