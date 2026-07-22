package me.noynto.eosa.product;

import me.noynto.eosa.shared.ImageId;
import me.noynto.eosa.shared.OptionId;
import me.noynto.eosa.shared.OptionValueId;
import me.noynto.eosa.shared.VariantId;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Variant {
    private VariantId id;
    private Map<OptionId, OptionValueId> optionValues = new HashMap<>();
    private BigDecimal price;
    private VariantState state;
    private int stock;
    private List<ImageId> imageIds = new ArrayList<>();

    public Variant() {
    }

    public VariantId getId() {
        return id;
    }

    public void setId(VariantId id) {
        this.id = id;
    }

    public Map<OptionId, OptionValueId> getOptionValues() {
        return optionValues;
    }

    public void setOptionValues(Map<OptionId, OptionValueId> optionValues) {
        this.optionValues = optionValues;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public VariantState getState() {
        return state;
    }

    public void setState(VariantState state) {
        this.state = state;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public List<ImageId> getImageIds() {
        return imageIds;
    }

    public void setImageIds(List<ImageId> imageIds) {
        this.imageIds = imageIds;
    }
}
