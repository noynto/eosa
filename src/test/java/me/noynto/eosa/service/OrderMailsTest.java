package me.noynto.eosa.service;

import me.noynto.eosa.infrastructure.mail.MailTemplates;
import me.noynto.eosa.order.Order;
import me.noynto.eosa.order.OrderItem;
import me.noynto.eosa.order.OrderShippingAddress;
import me.noynto.eosa.order.OrderState;
import me.noynto.eosa.shared.OrderId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderMailsTest {

    private final MailTemplates templates = new MailTemplates();

    @Test
    void confirmation_goesToTheCustomerWithNumberLinesAmountsAddressAndDelays() {
        var mail = OrderMails.confirmation(order());

        assertEquals("ada@example.com", mail.to());
        assertEquals("Votre commande EOSA EOSA-2026-0001", mail.subject());
        var rendered = templates.render(mail.template(), mail.model());
        for (String content : List.of(rendered.html(), rendered.text())) {
            assertTrue(content.contains("EOSA-2026-0001"));
            assertTrue(content.contains("Collier Lune"));
            assertTrue(content.contains("1 rue des Arts"));
            assertTrue(content.contains("54000 Nancy") || content.contains("54000"), content);
            assertTrue(content.contains("48 h"));
            assertTrue(content.contains("24 h"));
            assertTrue(content.contains("72 h"));
        }
        assertTrue(rendered.html().contains("70,90"));
        assertTrue(rendered.text().contains("70,90"));
        assertFalse(rendered.text().contains("<"), "la version texte ne contient pas de HTML");
    }

    @Test
    void confirmation_escapesWhatTheCustomerTyped() {
        var order = order();
        order.setCustomerName("Ada <script>alert(1)</script>");

        var mail = OrderMails.confirmation(order);
        var rendered = templates.render(mail.template(), mail.model());

        assertFalse(rendered.html().contains("<script>"));
        assertTrue(rendered.html().contains("&lt;script&gt;"));
    }

    @Test
    void confirmation_worksWithoutNameNorAddress() {
        var order = order();
        order.setCustomerName(null);
        order.setShippingAddress(null);

        var mail = OrderMails.confirmation(order);
        var rendered = templates.render(mail.template(), mail.model());

        assertTrue(rendered.html().contains("EOSA-2026-0001"));
        assertFalse(rendered.html().contains("Adresse de livraison"));
    }

    @Test
    void notification_goesToEosaWithTheAdminLinkAndTheCraftingDeadline() {
        var mail = OrderMails.notification(order(), URI.create("https://admin.eosa.me"));

        assertEquals("contact@eosa.me", mail.to());
        assertEquals("Nouvelle commande EOSA-2026-0001 à fabriquer", mail.subject());
        assertEquals("https://admin.eosa.me/orders/order-1", mail.model().get("orderUrl"));
        assertEquals(2, mail.model().get("pieces"));
        // Payée le jeudi 12 novembre 2026 à 11 h 15 : l'échéance saute le week-end et tombe le lundi 16 novembre.
        var rendered = templates.render(mail.template(), mail.model());
        assertTrue(rendered.html().contains("lundi 16 novembre 2026"));
        assertTrue(rendered.html().contains("https://admin.eosa.me/orders/order-1"));
        assertTrue(rendered.text().contains("https://admin.eosa.me/orders/order-1"));
        assertTrue(rendered.html().contains("Ada Lovelace"));
        assertTrue(rendered.text().contains("2 × Collier Lune"));
    }

    private Order order() {
        var item = new OrderItem();
        item.setName("Collier Lune");
        item.setUnitPrice(new BigDecimal("33.00"));
        item.setQuantity(2);

        var order = new Order();
        order.setId(new OrderId("order-1"));
        order.setNumber("EOSA-2026-0001");
        order.setState(OrderState.PAID);
        order.setCustomerName("Ada Lovelace");
        order.setCustomerEmail("ada@example.com");
        order.setShippingAddress(new OrderShippingAddress("Ada Lovelace", "1 rue des Arts", null, "54000", "Nancy", "FR"));
        order.setItems(List.of(item));
        order.setItemsAmount(new BigDecimal("66.00"));
        order.setShippingAmount(new BigDecimal("4.90"));
        order.setTotalAmount(new BigDecimal("70.90"));
        order.setPaidAt(Instant.parse("2026-11-12T10:15:00Z"));
        return order;
    }

}
