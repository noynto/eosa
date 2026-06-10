package me.noynto.eosa.cart;

import me.noynto.eosa.shared.CartId;

import java.util.ArrayList;
import java.util.List;

public class Cart {
    private CartId id;
    private List<CartItem> items = new ArrayList<>();

    public CartId getId() {
        return id;
    }

    public void setId(CartId id) {
        this.id = id;
    }

    public List<CartItem> getItems() {
        return items;
    }

    public void setItems(List<CartItem> items) {
        this.items = items;
    }
}