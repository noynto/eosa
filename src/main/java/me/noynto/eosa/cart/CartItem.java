package me.noynto.eosa.cart;

import me.noynto.eosa.shared.CharmId;
import me.noynto.eosa.shared.ImageId;
import me.noynto.eosa.shared.VariantId;

import java.math.BigDecimal;

public class CartItem {
    private VariantId variantId;
    private CharmId charmId;
    private String name;
    private BigDecimal price;
    private BigDecimal charmAdditionalPrice;
    private ImageId imageId;
    private int quantity;

    public CartItem() {
    }

    public CartItem(
            VariantId variantId,
            CharmId charmId,
            String name,
            BigDecimal price,
            BigDecimal charmAdditionalPrice,
            ImageId imageId,
            int quantity
    ) {
        this.variantId = variantId;
        this.charmId = charmId;
        this.name = name;
        this.price = price;
        this.charmAdditionalPrice = charmAdditionalPrice;
        this.imageId = imageId;
        this.quantity = quantity;
    }

    public VariantId variantId() {
        return variantId;
    }

    public CharmId charmId() {
        return charmId;
    }

    public String name() {
        return name;
    }

    public BigDecimal price() {
        return price;
    }

    public BigDecimal charmAdditionalPrice() {
        return charmAdditionalPrice;
    }

    public ImageId imageId() {
        return imageId;
    }

    public int quantity() {
        return quantity;
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

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
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

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
