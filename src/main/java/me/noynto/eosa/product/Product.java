package me.noynto.eosa.product;

import me.noynto.eosa.shared.ImageId;
import me.noynto.eosa.shared.ProductId;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Product {
    private ProductId id;
    private String name;
    private String tagline;
    private BigDecimal price;
    private List<ImageId> imageIds = new ArrayList<>();
    private ProductCategory category;
    private ProductState state;

    public Product() {
    }

    public ProductId getId() {
        return id;
    }

    public void setId(ProductId id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String description) {
        this.tagline = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public List<ImageId> getImageIds() {
        return imageIds;
    }

    public void setImageIds(List<ImageId> imageIds) {
        this.imageIds = imageIds;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public void setCategory(ProductCategory category) {
        this.category = category;
    }

    public ProductState getState() {
        return state;
    }

    public void setState(ProductState state) {
        this.state = state;
    }
}
