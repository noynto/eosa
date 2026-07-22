package me.noynto.eosa.product;

import me.noynto.eosa.shared.OptionId;
import me.noynto.eosa.shared.ProductId;
import me.noynto.eosa.shared.VariantId;

import java.util.ArrayList;
import java.util.List;

public class Product {
    private ProductId id;
    private String name;
    private String description;
    private ProductState state;
    private List<OptionId> optionIds = new ArrayList<>();
    private List<Variant> variants = new ArrayList<>();
    private VariantId defaultVariantId;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ProductState getState() {
        return state;
    }

    public void setState(ProductState state) {
        this.state = state;
    }

    public List<OptionId> getOptionIds() {
        return optionIds;
    }

    public void setOptionIds(List<OptionId> optionIds) {
        this.optionIds = optionIds;
    }

    public List<Variant> getVariants() {
        return variants;
    }

    public void setVariants(List<Variant> variants) {
        this.variants = variants;
    }

    public VariantId getDefaultVariantId() {
        return defaultVariantId;
    }

    public void setDefaultVariantId(VariantId defaultVariantId) {
        this.defaultVariantId = defaultVariantId;
    }
}
