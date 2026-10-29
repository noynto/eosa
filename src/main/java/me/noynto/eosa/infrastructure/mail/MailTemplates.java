package me.noynto.eosa.infrastructure.mail;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;

import java.io.StringWriter;
import java.util.Map;

/**
 * Gabarits Mustache des e-mails : {@code templates/mail/<nom>.html.mustache} et
 * {@code templates/mail/<nom>.txt.mustache}. Les partials partagés (en-tête, pied) sont dans
 * {@code templates/mail/partials/}.
 */
public class MailTemplates {

    private final MustacheFactory mustacheFactory = new DefaultMustacheFactory("templates");

    public Rendered render(String template, Map<String, Object> model) {
        return new Rendered(
                renderFile("mail/" + template + ".html.mustache", model),
                renderFile("mail/" + template + ".txt.mustache", model)
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
