package me.noynto.eosa.cart;

import me.noynto.eosa.shared.CartItemId;
import me.noynto.eosa.shared.ImageId;
import me.noynto.eosa.shared.JewelId;
import me.noynto.eosa.shared.MetalColorId;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CartItem {
    private CartItemId id;
    private JewelId jewelId;
    private String name;
    private BigDecimal price;
    private ImageId imageId;
    private int quantity;
    private MetalColorId metalColorId;
    private String metalColorName;
    private ImageId metalColorImageId;
    private List<SelectedCharm> charms = new ArrayList<>();

    public CartItem() {
    }

    public CartItem(
            CartItemId id,
            JewelId jewelId,
            String name,
            BigDecimal price,
            ImageId imageId,
            int quantity,
            MetalColorId metalColorId,
            String metalColorName,
            ImageId metalColorImageId,
            List<SelectedCharm> charms
    ) {
        this.id = id;
        this.jewelId = jewelId;
        this.name = name;
        this.price = price;
        this.imageId = imageId;
        this.quantity = quantity;
        this.metalColorId = metalColorId;
        this.metalColorName = metalColorName;
        this.metalColorImageId = metalColorImageId;
        this.charms = charms != null ? charms : new ArrayList<>();
    }

    public CartItemId id() {
        return id;
    }

    public JewelId jewelId() {
        return jewelId;
    }

    public String name() {
        return name;
    }

    public BigDecimal price() {
        return price;
    }

    public ImageId imageId() {
        return imageId;
    }

    public int quantity() {
        return quantity;
    }

    public MetalColorId metalColorId() {
        return metalColorId;
    }

    public String metalColorName() {
        return metalColorName;
    }

    public ImageId metalColorImageId() {
        return metalColorImageId;
    }

    public List<SelectedCharm> charms() {
        return charms;
    }

    public BigDecimal effectiveUnitPrice() {
        return charms.stream()
                .map(SelectedCharm::price)
                .reduce(price, BigDecimal::add);
    }

    public CartItemId getId() {
        return id;
    }

    public void setId(CartItemId id) {
        this.id = id;
    }

    public JewelId getJewelId() {
        return jewelId;
    }

    public void setJewelId(JewelId jewelId) {
        this.jewelId = jewelId;
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

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public MetalColorId getMetalColorId() {
        return metalColorId;
    }

    public void setMetalColorId(MetalColorId metalColorId) {
        this.metalColorId = metalColorId;
    }

    public String getMetalColorName() {
        return metalColorName;
    }

    public void setMetalColorName(String metalColorName) {
        this.metalColorName = metalColorName;
    }

    public ImageId getMetalColorImageId() {
        return metalColorImageId;
    }

    public void setMetalColorImageId(ImageId metalColorImageId) {
        this.metalColorImageId = metalColorImageId;
    }

    public List<SelectedCharm> getCharms() {
        return charms;
    }

    public void setCharms(List<SelectedCharm> charms) {
        this.charms = charms != null ? charms : new ArrayList<>();
    }
}
