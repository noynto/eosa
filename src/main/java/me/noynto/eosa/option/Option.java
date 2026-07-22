package me.noynto.eosa.option;

import me.noynto.eosa.shared.OptionId;

import java.util.ArrayList;
import java.util.List;

public class Option {
    private OptionId id;
    private String name;
    private String introText;
    private List<OptionValue> values = new ArrayList<>();

    public Option() {
    }

    public OptionId getId() {
        return id;
    }

    public void setId(OptionId id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIntroText() {
        return introText;
    }

    public void setIntroText(String introText) {
        this.introText = introText;
    }

    public List<OptionValue> getValues() {
        return values;
    }

    public void setValues(List<OptionValue> values) {
        this.values = values;
    }
}
