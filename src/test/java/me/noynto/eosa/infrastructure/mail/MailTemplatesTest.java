package me.noynto.eosa.infrastructure.mail;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MailTemplatesTest {

    @Test
    void render_producesHtmlWithTheCommonLayoutAndPlainText() {
        var rendered = new MailTemplates().render("test", Map.of("preheader", "Aperçu <du> message"));

        assertTrue(rendered.html().startsWith("<!DOCTYPE html>"));
        assertTrue(rendered.html().contains("Test d'envoi"));
        assertTrue(rendered.html().contains("Aperçu &lt;du&gt; message"), "les valeurs sont échappées dans le HTML");
        assertTrue(rendered.html().contains("@eosa.bijoux"));
        assertTrue(rendered.text().contains("Test d'envoi"));
        assertTrue(rendered.text().contains("https://www.instagram.com/eosa.bijoux"));
        assertFalse(rendered.text().contains("<"), "la version texte ne contient pas de HTML");
    }

}
