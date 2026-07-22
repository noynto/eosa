package me.noynto.eosa.charm;

import me.noynto.eosa.shared.CharmId;
import me.noynto.eosa.shared.ImageId;

import java.math.BigDecimal;

public class Charm {
    private CharmId id;
    private String name;
    private String description;
    private ImageId imageId;
    private BigDecimal additionalPrice;
    private int stock;
    private CharmState state;

    public Charm() {
    }

    public CharmId getId() {
        return id;
    }

    public void setId(CharmId id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ImageId getImageId() {
        return imageId;
    }

    public void setImageId(ImageId imageId) {
        this.imageId = imageId;
    }

    public BigDecimal getAdditionalPrice() {
        return additionalPrice;
    }

    public void setAdditionalPrice(BigDecimal additionalPrice) {
        this.additionalPrice = additionalPrice;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public CharmState getState() {
        return state;
    }

    public void setState(CharmState state) {
        this.state = state;
    }
}
