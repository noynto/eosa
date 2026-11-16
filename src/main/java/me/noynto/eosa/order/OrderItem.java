package me.noynto.eosa.order;

import me.noynto.eosa.shared.ImageId;
import me.noynto.eosa.shared.JewelId;

import java.math.BigDecimal;

/**
 * Ligne copiée au paiement : modifier ou supprimer le bijou d'origine ne la change jamais.
 */
public class OrderItem {

    private String name;
    private BigDecimal unitPrice;
    private int quantity;
    private JewelId jewelId;
    private ImageId imageId;

    public OrderItem() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public JewelId getJewelId() {
        return jewelId;
    }

    public void setJewelId(JewelId jewelId) {
        this.jewelId = jewelId;
    }

    public ImageId getImageId() {
        return imageId;
    }

    public void setImageId(ImageId imageId) {
        this.imageId = imageId;
    }

    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
