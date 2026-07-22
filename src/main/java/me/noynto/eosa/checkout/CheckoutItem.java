package me.noynto.eosa.checkout;

import me.noynto.eosa.shared.CharmId;
import me.noynto.eosa.shared.CheckoutItemId;
import me.noynto.eosa.shared.ImageId;
import me.noynto.eosa.shared.VariantId;

import java.math.BigDecimal;

public class CheckoutItem {
    private CheckoutItemId id;
    private VariantId variantId;
    private CharmId charmId;
    private String name;
    private BigDecimal unitPrice;
    private BigDecimal charmAdditionalPrice;
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

    public VariantId getVariantId() {
        return variantId;
    }

    public void setVariantId(VariantId variantId) {
        this.variantId = variantId;
    }

    public CharmId getCharmId() {
        return charmId;
    }

    public void setCharmId(CharmId charmId) {
        this.charmId = charmId;
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

    public BigDecimal getCharmAdditionalPrice() {
        return charmAdditionalPrice;
    }

    public void setCharmAdditionalPrice(BigDecimal charmAdditionalPrice) {
        this.charmAdditionalPrice = charmAdditionalPrice;
    }

    public ImageId getImageId() {
        return imageId;
    }

    public void setImageId(ImageId imageId) {
        this.imageId = imageId;
    }
}
