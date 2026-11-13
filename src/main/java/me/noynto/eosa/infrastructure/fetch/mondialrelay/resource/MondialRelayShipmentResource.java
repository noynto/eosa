package me.noynto.eosa.infrastructure.fetch.mondialrelay.resource;

import me.noynto.eosa.infrastructure.fetch.mondialrelay.config.MondialRelayConnectProperties;
import me.noynto.eosa.shipping.ShippingLabel;
import me.noynto.eosa.shipping.ShippingRecipient;
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
import java.text.Normalizer;
import java.time.Duration;
import java.util.Locale;

/**
 * Création d'un envoi et de son étiquette par l'API Connect de Mondial Relay (REST en XML, document « Web service
 * dual carrier »). Une seule méthode existe : {@code POST /api/shipment}. Livraison à domicile (HOM), collecte par
 * l'expéditeur (CCC), étiquette PDF 10x15.
 */
public record MondialRelayShipmentResource(
        HttpClient client,
        MondialRelayConnectProperties properties
) {

    /** Limites imposées par le transporteur : poids de l'étiquette à domicile, en grammes. */
    public static final int MIN_WEIGHT_GRAMS = 10;
    public static final int MAX_WEIGHT_GRAMS = 30_000;

    public Created create(ShippingLabel label) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(properties.endpoint())
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/xml")
                .header("Content-Type", "text/xml")
                .POST(HttpRequest.BodyPublishers.ofString(buildRequest(properties, label), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() / 100 != 2 && response.body().isBlank()) {
            throw new IOException("Mondial Relay a répondu " + response.statusCode() + ".");
        }
        return parseResponse(response.body());
    }

    public record Created(String shipmentNumber, String labelUrl) {
    }

    static String buildRequest(MondialRelayConnectProperties properties, ShippingLabel label) {
        ShippingRecipient recipient = label.getRecipient();
        MondialRelayConnectProperties.Sender sender = properties.sender();
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        xml.append("<ShipmentCreationRequest xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ")
                .append("xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns=\"http://www.example.org/Request\">\n");
        xml.append("<Context>");
        element(xml, "Login", properties.login());
        element(xml, "Password", properties.password());
        element(xml, "CustomerId", properties.customerId());
        element(xml, "Culture", "fr-FR");
        element(xml, "VersionAPI", "1.0");
        xml.append("</Context>\n");
        xml.append("<OutputOptions>");
        element(xml, "OutputFormat", "10x15");
        element(xml, "OutputType", "PdfUrl");
        xml.append("</OutputOptions>\n");
        xml.append("<ShipmentsList><Shipment>");
        element(xml, "OrderNo", clean(label.getOrderNumber(), 15));
        element(xml, "CustomerNo", "");
        element(xml, "ParcelCount", "1");
        xml.append("<DeliveryMode Mode=\"HOM\" Location=\"\" />");
        xml.append("<CollectionMode Mode=\"CCC\" Location=\"\" />");
        xml.append("<Parcels><Parcel>");
        element(xml, "Content", "BIJOU");
        // Les dimensions sont obligatoires (sinon WebApi_MandatoryParcelSize).
        xml.append("<Length Value=\"").append(label.getLengthCm()).append("\" Unit=\"cm\" />");
        xml.append("<Width Value=\"").append(label.getWidthCm()).append("\" Unit=\"cm\" />");
        xml.append("<Depth Value=\"").append(label.getDepthCm()).append("\" Unit=\"cm\" />");
        xml.append("<Weight Value=\"").append(label.getWeightGrams()).append("\" Unit=\"gr\" />");
        xml.append("</Parcel></Parcels>");
        xml.append("<Sender><Address>");
        element(xml, "Streetname", clean(sender.line1(), 40));
        element(xml, "CountryCode", "FR");
        element(xml, "PostCode", clean(sender.postalCode(), 10));
        element(xml, "City", clean(sender.city(), 30));
        element(xml, "AddressAdd1", clean(sender.name(), 30));
        element(xml, "PhoneNo", phone(sender.phone()));
        element(xml, "Email", sender.email() == null ? "" : sender.email());
        xml.append("</Address></Sender>");
        xml.append("<Recipient><Address>");
        element(xml, "Streetname", clean(recipient.line1(), 40));
        element(xml, "CountryCode", clean(recipient.country(), 2));
        element(xml, "PostCode", clean(recipient.postalCode(), 10));
        element(xml, "City", clean(recipient.city(), 30));
        element(xml, "AddressAdd1", clean(recipient.name(), 30));
        element(xml, "AddressAdd3", clean(recipient.line2(), 30));
        element(xml, "PhoneNo", phone(recipient.phone()));
        element(xml, "Email", recipient.email() == null ? "" : recipient.email());
        xml.append("</Address></Recipient>");
        xml.append("</Shipment></ShipmentsList>\n");
        xml.append("</ShipmentCreationRequest>");
        return xml.toString();
    }

    static Created parseResponse(String body) throws IOException {
        Document document = parse(body);
        NodeList statuses = document.getElementsByTagName("Status");
        for (int i = 0; i < statuses.getLength(); i++) {
            Element status = (Element) statuses.item(i);
            String level = attribute(status, "Level").toLowerCase(Locale.ROOT);
            if (level.contains("error") || level.contains("critical")) {
                String code = attribute(status, "Code");
                String message = attribute(status, "Message");
                throw new RuntimeException("Mondial Relay a refusé l'étiquette : "
                        + (message.isBlank() ? "erreur sans message" : message)
                        + (code.isBlank() ? "" : " (code " + code + ")") + ".");
            }
        }
        NodeList shipments = document.getElementsByTagName("Shipment");
        NodeList outputs = document.getElementsByTagName("Output");
        if (shipments.getLength() == 0 || outputs.getLength() == 0) {
            throw new RuntimeException("Mondial Relay n'a pas rendu d'étiquette.");
        }
        String shipmentNumber = ((Element) shipments.item(0)).getAttribute("ShipmentNumber").strip();
        String labelUrl = outputs.item(0).getTextContent().strip();
        if (shipmentNumber.isEmpty() || labelUrl.isEmpty()) {
            throw new RuntimeException("Mondial Relay n'a pas rendu d'étiquette.");
        }
        return new Created(shipmentNumber, labelUrl);
    }

    private static Document parse(String body) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // La réponse vient d'un tiers : ni DTD, ni entités externes.
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            // Lue depuis une chaîne : la déclaration d'encodage de la réponse (utf-16) ne compte pas.
            return factory.newDocumentBuilder().parse(new InputSource(new StringReader(body)));
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException("Réponse illisible de Mondial Relay.", e);
        }
    }

    /** Un attribut sans tenir compte de la casse de son nom, vide s'il est absent. */
    private static String attribute(Element element, String name) {
        var attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            if (attributes.item(i).getNodeName().equalsIgnoreCase(name)) {
                return attributes.item(i).getNodeValue().strip();
            }
        }
        return "";
    }

    private static void element(StringBuilder xml, String name, String value) {
        xml.append('<').append(name).append('>').append(escape(value == null ? "" : value)).append("</").append(name).append('>');
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    /**
     * Le transporteur n'accepte que des majuscules sans accent, des chiffres et quelques signes, avec une longueur
     * maximale : tout le reste devient une espace, puis le texte est tronqué.
     */
    static String clean(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String cleaned = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replace("Œ", "OE")
                .replace("Æ", "AE")
                .replaceAll("[^0-9A-Z_\\-'., /]", " ")
                .replaceAll("\\s+", " ")
                .strip();
        return cleaned.length() > maxLength ? cleaned.substring(0, maxLength).strip() : cleaned;
    }

    private static String phone(String value) {
        return value == null ? "" : value.replaceAll("[\\s.\\-()]", "");
    }

}
