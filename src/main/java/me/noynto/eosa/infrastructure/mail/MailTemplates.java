package me.noynto.eosa.infrastructure.mail;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;

import java.io.StringWriter;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Gabarits Mustache des e-mails : {@code templates/mail/<nom>.html.mustache} et
 * {@code templates/mail/<nom>.txt.mustache}. Les partials partagés (en-tête, pied) sont dans
 * {@code templates/mail/partials/}.
 * <p>
 * Chaque modèle reçoit en plus {@code baseUrl} (adresse publique du site) et {@code logoUrl} (le logo en image, que
 * l'en-tête affiche : Gmail et Outlook ne chargent pas les polices web). Un service d'e-mail ne les fournit jamais.
 */
public class MailTemplates {

    private static final String LOGO_PATH = "/mail/logo.png";

    private final MustacheFactory mustacheFactory = new DefaultMustacheFactory("templates");
    private final String baseUrl;

    public MailTemplates(URI publicBaseUrl) {
        this.baseUrl = publicBaseUrl.toString().replaceAll("/+$", "");
    }

    public Rendered render(String template, Map<String, Object> model) {
        // Le modèle d'un e-mail n'est pas forcément modifiable : on en prend une copie.
        Map<String, Object> fullModel = new HashMap<>(model);
        fullModel.putIfAbsent("baseUrl", baseUrl);
        fullModel.putIfAbsent("logoUrl", baseUrl + LOGO_PATH);
        return new Rendered(
                renderFile("mail/" + template + ".html.mustache", fullModel),
                renderFile("mail/" + template + ".txt.mustache", fullModel)
        );
    }

    private String renderFile(String path, Map<String, Object> model) {
        StringWriter writer = new StringWriter();
        mustacheFactory.compile(path).execute(writer, model);
        return writer.toString();
    }

    public record Rendered(
            String html,
            String text
    ) {
    }

}
