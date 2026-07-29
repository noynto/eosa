package me.noynto.eosa.jewel;

import me.noynto.eosa.shared.ImageId;
import me.noynto.eosa.shared.JewelId;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Jewel {
    private JewelId id;
    private String name;
    private String tagline;
    private BigDecimal price;
    private List<ImageId> imageIds = new ArrayList<>();
    private JewelCategory category;
    private JewelState state;

    public Jewel() {
    }

    public JewelId getId() {
        return id;
    }

    public void setId(JewelId id) {
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

    public JewelCategory getCategory() {
        return category;
    }

    public void setCategory(JewelCategory category) {
        this.category = category;
    }

    public JewelState getState() {
        return state;
    }

    public void setState(JewelState state) {
        this.state = state;
    }
}
