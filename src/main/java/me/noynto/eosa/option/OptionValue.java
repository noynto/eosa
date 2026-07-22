package me.noynto.eosa.option;

import me.noynto.eosa.shared.OptionValueId;

public class OptionValue {
    private OptionValueId id;
    private String label;
    private String description;

    public OptionValue() {
    }

    public OptionValueId getId() {
        return id;
    }

    public void setId(OptionValueId id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
