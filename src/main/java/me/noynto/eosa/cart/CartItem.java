package me.noynto.eosa.cart;

import me.noynto.eosa.shared.ImageId;
import me.noynto.eosa.shared.ProductId;

import java.math.BigDecimal;
import java.util.Objects;

public class CartItem {
    private ProductId productId;
    private String name;
    private BigDecimal price;
    private ImageId imageId;
    private int quantity;

    public CartItem() {
    }

    public CartItem(
            ProductId productId,
            String name,
            BigDecimal price,
            ImageId imageId,
            int quantity
    ) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.imageId = imageId;
        this.quantity = quantity;
    }

    public ProductId productId() {
        return productId;
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