package me.noynto.eosa.infrastructure.mail;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MailTemplatesTest {

    private final MailTemplates templates = new MailTemplates(URI.create("https://eosa.me"));

    @Test
    void render_producesHtmlWithTheCommonLayoutAndPlainText() {
        var rendered = templates.render("test", Map.of("preheader", "Aperçu <du> message"));

        assertTrue(rendered.html().startsWith("<!DOCTYPE html>"));
        assertTrue(rendered.html().contains("Test d'envoi"));
        assertTrue(rendered.html().contains("Aperçu &lt;du&gt; message"), "les valeurs sont échappées dans le HTML");
        assertTrue(rendered.html().contains("@eosa.bijoux"));
        assertTrue(rendered.text().contains("Test d'envoi"));
        assertTrue(rendered.text().contains("https://www.instagram.com/eosa.bijoux"));
        assertFalse(rendered.text().contains("<"), "la version texte ne contient pas de HTML");
    }

    @Test
    void render_showsTheLogoAsAnImageWithAnAlternativeText() {
        var rendered = templates.render("test", Map.of("preheader", ""));

        assertTrue(rendered.html().contains("<img src=\"https://eosa.me/mail/logo.png\""));
        assertTrue(rendered.html().contains("alt=\"eosa.\""));
    }

    @Test
    void render_loadsTheBrandFontsFromTheSite() {
        var rendered = templates.render("test", Map.of("preheader", ""));

        assertTrue(rendered.html().contains("@font-face"));
        assertTrue(rendered.html().contains("https://eosa.me/fonts/"));
    }

    @Test
    void render_ignoresATrailingSlashOfTheBaseUrl() {
        var rendered = new MailTemplates(URI.create("https://eosa.me/")).render("test", Map.of("preheader", ""));

        assertTrue(rendered.html().contains("https://eosa.me/mail/logo.png"));
        assertFalse(rendered.html().contains("https://eosa.me//"));
    }

}
