package me.noynto.eosa.service;

import me.noynto.eosa.giftcard.GiftCard;
import me.noynto.eosa.infrastructure.mail.MailTemplates;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GiftCardMailsTest {

    private GiftCard card(String firstName) {
        var giftCard = new GiftCard();
        giftCard.setCode("ABCDEFGH2345");
        giftCard.setToken("tok-123");
        giftCard.setInitialAmount(new BigDecimal("50.00"));
        giftCard.setBuyerFirstName(firstName);
        giftCard.setBuyerEmail("lea@example.com");
        giftCard.setExpiresAt(Instant.parse("2027-11-16T23:00:00Z"));
        return giftCard;
    }

    @Test
    void delivery_goesToTheBuyerWithCodeValidityAndPrintableLink() {
        var mail = GiftCardMails.delivery(card("Léa"), URI.create("https://eosa.test/"));

        assertEquals("lea@example.com", mail.to());
        assertEquals("Votre carte cadeau EOSA", mail.subject());
        var rendered = new MailTemplates(URI.create("https://eosa.me")).render(mail.template(), mail.model());
        for (var body : new String[]{rendered.html(), rendered.text()}) {
            assertTrue(body.contains("ABCDEFGH2345"), body);
            assertTrue(body.contains("16 novembre 2027"), body);
            assertTrue(body.contains("https://eosa.test/gift-cards/tok-123"), body);
            assertTrue(body.contains("Merci Léa"), body);
            assertTrue(body.contains("TVA non applicable, art. 293 B du Code général des impôts"), body);
        }
        assertFalse(rendered.text().contains("<"), "la version texte ne contient pas de HTML");
    }

    @Test
    void delivery_worksWithoutAFirstName() {
        var mail = GiftCardMails.delivery(card(null), URI.create("https://eosa.test"));

        var rendered = new MailTemplates(URI.create("https://eosa.me")).render(mail.template(), mail.model());
        assertTrue(rendered.text().contains("Voici votre carte cadeau"), rendered.text());
    }

}
