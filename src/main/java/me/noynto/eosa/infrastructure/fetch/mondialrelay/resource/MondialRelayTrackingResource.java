package me.noynto.eosa.infrastructure.fetch.mondialrelay.resource;

import me.noynto.eosa.infrastructure.fetch.mondialrelay.config.MondialRelayTrackingProperties;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

/**
 * Suivi d'un colis par le web service V1 de Mondial Relay (SOAP, méthode {@code WSI2_TracingColisDetaille}).
 * Chaque appel est signé : MD5, en majuscules, de l'enseigne, du numéro d'expédition, de la langue et de la clé
 * privée mis bout à bout. La méthode ne fait que lire.
 */
public record MondialRelayTrackingResource(
        HttpClient client,
        MondialRelayTrackingProperties properties
) {

    private static final ZoneId ZONE = ZoneId.of("Europe/Paris");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    /** Code de statut du suivi : colis enregistré (étiquette créée), en cours chez Mondial Relay, livré, anomalie. */
    public static final int STAT_RECORDED = 80;
    public static final int STAT_IN_PROGRESS = 81;
    public static final int STAT_DELIVERED = 82;
    public static final int STAT_ANOMALY = 83;
    /** Colis inconnu de Mondial Relay. */
    public static final int STAT_UNKNOWN_PARCEL = 94;

    public Tracking track(String shipmentNumber) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(properties.endpoint())
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "text/xml; charset=utf-8")
                .header("SOAPAction", "\"http://www.mondialrelay.fr/webservice/WSI2_TracingColisDetaille\"")
                .POST(HttpRequest.BodyPublishers.ofString(buildRequest(properties, shipmentNumber), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return parseResponse(response.body());
    }

    /**
     * @param stat    code de statut de l'appel (voir les constantes {@code STAT_*})
     * @param message libellé du statut courant (« Livraison au point relais »), vide si absent
     * @param events  événements du colis, du plus ancien au plus récent
     */
    public record Tracking(
            int stat,
            String message,
            List<Event> events
    ) {
    }

    /**
     * @param label libellé de l'événement (« PRISE EN CHARGE POINT RELAIS »)
     * @param at    date et heure de l'événement, heure de Paris
     */
    public record Event(
            String label,
            Instant at
    ) {
    }

    static String buildRequest(MondialRelayTrackingProperties properties, String shipmentNumber) {
        if (shipmentNumber == null || !shipmentNumber.matches("[0-9]{8}")) {
            throw new IllegalArgumentException("Le numéro d'expédition doit avoir 8 chiffres : " + shipmentNumber);
        }
        String security = md5(properties.enseigne() + shipmentNumber + "FR" + properties.privateKey()).toUpperCase();
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ws=\"http://www.mondialrelay.fr/webservice/\">"
                + "<soap:Body><ws:WSI2_TracingColisDetaille>"
                + "<ws:Enseigne>" + escape(properties.enseigne()) + "</ws:Enseigne>"
                + "<ws:Expedition>" + shipmentNumber + "</ws:Expedition>"
                + "<ws:Langue>FR</ws:Langue>"
                + "<ws:Security>" + security + "</ws:Security>"
                + "</ws:WSI2_TracingColisDetaille></soap:Body></soap:Envelope>";
    }

    static Tracking parseResponse(String body) throws IOException {
        Document document = parse(body);
        String stat = text(document, "STAT");
        if (stat.isEmpty()) {
            throw new IOException("Réponse de suivi sans statut.");
        }
        List<Event> events = new ArrayList<>();
        NodeList nodes = document.getElementsByTagName("ret_WSI2_sub_TracingColisDetaille");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element node = (Element) nodes.item(i);
            // Le service complète la liste d'événements avec des entrées vides : on les ignore.
            String label = text(node, "Libelle");
            String date = text(node, "Date");
            String time = text(node, "Heure");
            if (label.isEmpty() || date.isEmpty()) {
                continue;
            }
            try {
                LocalDateTime local = LocalDateTime.of(
                        java.time.LocalDate.parse(date, DATE),
                        time.isEmpty() ? java.time.LocalTime.MIDNIGHT : java.time.LocalTime.parse(time, TIME));
                events.add(new Event(label, local.atZone(ZONE).toInstant()));
            } catch (DateTimeParseException e) {
                throw new IOException("Date de suivi illisible : " + date + " " + time, e);
            }
        }
        events.sort(Comparator.comparing(Event::at));
        try {
            return new Tracking(Integer.parseInt(stat), text(document, "Libelle01"), events);
        } catch (NumberFormatException e) {
            throw new IOException("Statut de suivi illisible : " + stat, e);
        }
    }

    private static Document parse(String body) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // La réponse vient d'un tiers : ni DTD, ni entités externes.
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            return factory.newDocumentBuilder().parse(new InputSource(new StringReader(body)));
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException("Réponse illisible de Mondial Relay.", e);
        }
    }

    private static String text(Document document, String tag) {
        NodeList nodes = document.getElementsByTagName(tag);
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().strip();
    }

    private static String text(Element element, String tag) {
        NodeList nodes = element.getElementsByTagName(tag);
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().strip();
    }

    private static String md5(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

}
