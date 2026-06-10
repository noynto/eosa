package me.noynto.eosa.shared;

public class ProductId {
    private String value;

    public ProductId() {
    }

    public ProductId(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}