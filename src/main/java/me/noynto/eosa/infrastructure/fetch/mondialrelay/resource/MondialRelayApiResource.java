package me.noynto.eosa.infrastructure.fetch.mondialrelay.resource;

import me.noynto.eosa.infrastructure.fetch.mondialrelay.config.MondialRelayProperties;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public record MondialRelayApiResource(
        HttpClient client,
        MondialRelayProperties properties
) {

    private static final URI ENDPOINT = URI.create("https://api.mondialrelay.com/WebService.asmx");

    public Stream<PointRelais> searchPickupPoints(String postalCode, String country, int radiusKm)
            throws IOException, InterruptedException {
        String security = computeSecurity(postalCode, country, radiusKm);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(ENDPOINT)
                .header("Content-Type", "text/xml; charset=utf-8")
                .header("SOAPAction", "\"http://www.mondialrelay.fr/webservice/WSI3_PointRelais_Recherche\"")
                .POST(HttpRequest.BodyPublishers.ofString(buildSoapRequest(postalCode, country, radiusKm, security), StandardCharsets.UTF_8))
                .build();
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        return parseResponse(response.body());
    }

    private String buildSoapRequest(String cp, String pays, int rayon, String security) {
        return """
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
                               xmlns:ws="http://www.mondialrelay.fr/webservice/">
                  <soap:Body>
                    <ws:WSI3_PointRelais_Recherche>
                      <ws:Enseigne>%s</ws:Enseigne>
                      <ws:Pays>%s</ws:Pays>
                      <ws:NumPointRelais></ws:NumPointRelais>
                      <ws:Ville></ws:Ville>
                      <ws:CP>%s</ws:CP>
                      <ws:Latitude></ws:Latitude>
                      <ws:Longitude></ws:Longitude>
                      <ws:Taille></ws:Taille>
                      <ws:Poids></ws:Poids>
                      <ws:Action></ws:Action>
                      <ws:DelaiEnvoi>0</ws:DelaiEnvoi>
                      <ws:RayonRecherche>%d</ws:RayonRecherche>
                      <ws:TypeActivite></ws:TypeActivite>
                      <ws:NACE></ws:NACE>
                      <ws:Security>%s</ws:Security>
                    </ws:WSI3_PointRelais_Recherche>
                  </soap:Body>
                </soap:Envelope>
                """.formatted(properties.enseigne(), pays, cp, rayon, security);
    }

    private String computeSecurity(String cp, String pays, int rayon) {
        // Concatenation order: Enseigne + Pays + NumPointRelais + Ville + CP + Lat + Lon + Taille + Poids + Action + DelaiEnvoi + Rayon + TypeActivite + NACE + PrivateKey
        String raw = properties.enseigne() + pays + "" + "" + cp + "" + "" + "" + "" + "" + "0" + rayon + "" + "" + properties.privateKey();
        return md5(raw).toUpperCase();
    }

    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Stream<PointRelais> parseResponse(InputStream body) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            Document doc = factory.newDocumentBuilder().parse(body);
            NodeList nodes = doc.getElementsByTagName("PointRelais_Details");
            List<PointRelais> result = new ArrayList<>();
            for (int i = 0; i < nodes.getLength(); i++) {
                result.add(PointRelais.from((Element) nodes.item(i)));
            }
            return result.stream();
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException("Failed to parse Mondial Relay response", e);
        }
    }

    public record PointRelais(
            String num,
            String name,
            String address,
            String postalCode,
            String city,
            String country,
            String latitude,
            String longitude,
            String distance,
            String photoUrl,
            String mapUrl,
            String type,
            OpeningHours openingHours
    ) {
        static PointRelais from(Element e) {
            return new PointRelais(
                    text(e, "Num"),
                    text(e, "LgAdr1"),
                    text(e, "LgAdr3"),
                    text(e, "CP"),
                    text(e, "Ville"),
                    text(e, "Pays"),
                    text(e, "Latitude"),
                    text(e, "Longitude"),
                    text(e, "Distance"),
                    text(e, "URL_Photo"),
                    text(e, "URL_Plan"),
                    text(e, "TypeActivite"),
                    new OpeningHours(
                            text(e, "Horaires_Lundi"),
                            text(e, "Horaires_Mardi"),
                            text(e, "Horaires_Mercredi"),
                            text(e, "Horaires_Jeudi"),
                            text(e, "Horaires_Vendredi"),
                            text(e, "Horaires_Samedi"),
                            text(e, "Horaires_Dimanche")
                    )
            );
        }

        private static String text(Element e, String tag) {
            NodeList nodes = e.getElementsByTagName(tag);
            if (nodes.getLength() == 0) return "";
            return nodes.item(0).getTextContent().trim();
        }
    }

    public record OpeningHours(
            String monday,
            String tuesday,
            String wednesday,
            String thursday,
            String friday,
            String saturday,
            String sunday
    ) {
    }

}