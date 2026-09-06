package me.noynto.eosa.charm;

import me.noynto.eosa.shared.CharmId;
import me.noynto.eosa.shared.ImageId;

import java.math.BigDecimal;

public class Charm {
    private CharmId id;
    private String name;
    private BigDecimal price;
    private ImageId imageId;

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
}
