package me.noynto.eosa.cart;

import me.noynto.eosa.shared.ImageId;
import me.noynto.eosa.shared.JewelId;

import java.math.BigDecimal;
import java.util.Objects;

public class CartItem {
    private JewelId jewelId;
    private String name;
    private BigDecimal price;
    private ImageId imageId;
    private int quantity;

    public CartItem() {
    }

    public CartItem(
            JewelId jewelId,
            String name,
            BigDecimal price,
            ImageId imageId,
            int quantity
    ) {
        this.jewelId = jewelId;
        this.name = name;
        this.price = price;
        this.imageId = imageId;
        this.quantity = quantity;
    }

    public JewelId jewelId() {
        return jewelId;
    }

    public String name() {
        return name;
    }

    public BigDecimal price() {
        return price;
    }

    public ImageId imageId() {
        return imageId;
    }

    public int quantity() {
        return quantity;
    }

    public JewelId getJewelId() {
        return jewelId;
    }

    public void setJewelId(JewelId jewelId) {
        this.jewelId = jewelId;
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