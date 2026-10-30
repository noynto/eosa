package me.noynto.eosa.mail;

import java.util.Map;

/**
 * Un e-mail à envoyer. Le contenu est produit par le gabarit {@code template} (versions HTML et texte)
 * à partir du {@code model}, dont toutes les valeurs sont déjà mises en forme.
 */
public record Mail(
        String to,
        String subject,
        String template,
        Map<String, Object> model
) {
}
