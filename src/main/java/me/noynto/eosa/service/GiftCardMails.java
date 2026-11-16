package me.noynto.eosa.service;

import me.noynto.eosa.giftcard.GiftCard;
import me.noynto.eosa.mail.Mail;

import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Les e-mails des cartes cadeau. Chaque méthode construit le {@link Mail} : l'envoi, et l'enregistrement qu'il est
 * parti, restent au cas d'usage.
 */
public final class GiftCardMails {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH);

    private GiftCardMails() {
    }

    /**
     * « Votre carte cadeau », à la personne qui l'a achetée : montant, code, validité et lien de la page imprimable.
     * Il sert aussi de reçu.
     */
    public static Mail delivery(GiftCard giftCard, URI publicBaseUrl) {
        String baseUrl = publicBaseUrl.toString().replaceAll("/+$", "");
        String firstName = giftCard.getBuyerFirstName();
        Map<String, Object> model = new HashMap<>();
        model.put("preheader", "Votre carte cadeau EOSA de " + OrderMails.amount(giftCard.getInitialAmount()) + " est prête.");
        model.put("hasName", firstName != null && !firstName.isBlank());
        model.put("name", firstName);
        model.put("amount", OrderMails.amount(giftCard.getInitialAmount()));
        model.put("code", giftCard.getCode());
        model.put("validUntil", DATE.format(giftCard.getExpiresAt().atZone(BusinessTime.ZONE).toLocalDate().minusDays(1)));
        model.put("printUrl", baseUrl + "/gift-cards/" + giftCard.getToken());
        model.put("shopUrl", baseUrl + "/jewels");
        model.put("contactEmail", OrderMails.NOTIFICATION_RECIPIENT);
        return new Mail(giftCard.getBuyerEmail(), "Votre carte cadeau EOSA", "gift-card", model);
    }

}
