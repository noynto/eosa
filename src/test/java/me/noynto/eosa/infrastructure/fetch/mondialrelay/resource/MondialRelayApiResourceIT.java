package me.noynto.eosa.infrastructure.fetch.mondialrelay.resource;

import me.noynto.eosa.infrastructure.fetch.mondialrelay.config.MondialRelayProperties;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MondialRelayApiResourceIT {

    // Test credentials from Mondial Relay account (API 1 — test environment)
    private static final MondialRelayProperties SANDBOX = new MondialRelayProperties("TTMRSDBX", "9ytnxVCC");

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.of(10, ChronoUnit.SECONDS))
            .build();

    private final MondialRelayApiResource resource = new MondialRelayApiResource(HTTP_CLIENT, SANDBOX);

    @Test
    void rawSoapResponse_forDiagnostics() throws Exception {
        // Raw WSI3_PointRelais_Recherche request — Security hash computed manually for debug
        // Hash = MD5(TTMRSDBX + FR + "" + "" + 54000 + "" + "" + "" + "" + "" + 0 + 10 + "" + "" + 9ytnxVCC)
        String body = """
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
                               xmlns:ws="http://www.mondialrelay.fr/webservice/">
                  <soap:Body>
                    <ws:WSI3_PointRelais_Recherche>
                      <ws:Enseigne>TTMRSDBX</ws:Enseigne>
                      <ws:Pays>FR</ws:Pays>
                      <ws:NumPointRelais></ws:NumPointRelais>
                      <ws:Ville></ws:Ville>
                      <ws:CP>54000</ws:CP>
                      <ws:Latitude></ws:Latitude>
                      <ws:Longitude></ws:Longitude>
                      <ws:Taille></ws:Taille>
                      <ws:Poids></ws:Poids>
                      <ws:Action></ws:Action>
                      <ws:DelaiEnvoi>0</ws:DelaiEnvoi>
                      <ws:RayonRecherche>10</ws:RayonRecherche>
                      <ws:TypeActivite></ws:TypeActivite>
                      <ws:NACE></ws:NACE>
                      <ws:Security>COMPUTED_BY_RESOURCE</ws:Security>
                    </ws:WSI3_PointRelais_Recherche>
                  </soap:Body>
                </soap:Envelope>
                """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.mondialrelay.com/WebService.asmx"))
                .header("Content-Type", "text/xml; charset=utf-8")
                .header("SOAPAction", "\"http://www.mondialrelay.fr/webservice/WSI3_PointRelais_Recherche\"")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("STATUS HTTP : " + response.statusCode());
        System.out.println("BODY :\n" + response.body());
    }

    @Test
    void searchPickupPoints_returnsResultsForNancy() throws Exception {
        List<MondialRelayApiResource.PointRelais> results = resource
                .searchPickupPoints("54000", "FR", 10)
                .toList();

        assertFalse(results.isEmpty());
        MondialRelayApiResource.PointRelais first = results.getFirst();
        assertNotNull(first.num());
        assertFalse(first.name().isBlank());
        assertFalse(first.postalCode().isBlank());
        assertFalse(first.city().isBlank());
    }

    @Test
    void searchPickupPoints_fieldsAreMapped() throws Exception {
        MondialRelayApiResource.PointRelais point = resource
                .searchPickupPoints("54000", "FR", 10)
                .findFirst()
                .orElseThrow();

        System.out.println("Num       : " + point.num());
        System.out.println("Nom       : " + point.name());
        System.out.println("Adresse   : " + point.address());
        System.out.println("CP        : " + point.postalCode());
        System.out.println("Ville     : " + point.city());
        System.out.println("Type      : " + point.type());
        System.out.println("Latitude  : " + point.latitude());
        System.out.println("Longitude : " + point.longitude());
        System.out.println("Distance  : " + point.distance());
        System.out.println("Photo     : " + point.photoUrl());
        System.out.println("Plan      : " + point.mapUrl());
        System.out.println("Lundi     : " + point.openingHours().monday());
        System.out.println("Samedi    : " + point.openingHours().saturday());
        System.out.println("Dimanche  : " + point.openingHours().sunday());
    }

}