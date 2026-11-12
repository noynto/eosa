package me.noynto.eosa.infrastructure.fetch.mondialrelay.resource;

import me.noynto.eosa.infrastructure.fetch.mondialrelay.config.MondialRelayTrackingProperties;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MondialRelayTrackingResourceTest {

    private final MondialRelayTrackingProperties properties = new MondialRelayTrackingProperties(
            URI.create("https://api.mondialrelay.com/WebService.asmx"), "BDTEST13", "TestAPI1key");

    @Test
    void buildRequest_signsTheCallWithTheMd5OfEnseigneShipmentLanguageAndKey() {
        var xml = MondialRelayTrackingResource.buildRequest(properties, "12345678");

        // MD5 de « BDTEST13 » + « 12345678 » + « FR » + « TestAPI1key ». La documentation V1 donne 33DA5F12… pour
        // cette même chaîne, mais son exemple est périmé (la clé de test a changé en 2024) ; la signature a été
        // validée en réel : Mondial Relay a rendu le suivi d'un colis avec cette construction.
        assertTrue(xml.contains("<ws:Security>C3CE60FD2CE00764A00CEF8175E8D51D</ws:Security>"), xml);
        assertTrue(xml.contains("<ws:Enseigne>BDTEST13</ws:Enseigne>"));
        assertTrue(xml.contains("<ws:Expedition>12345678</ws:Expedition>"));
        assertTrue(xml.contains("<ws:Langue>FR</ws:Langue>"));
        assertFalse(xml.contains("TestAPI1key"), "la clé privée ne part jamais en clair");
    }

    @Test
    void buildRequest_keepsTheLeadingZerosAndRefusesAnythingButEightDigits() {
        assertTrue(MondialRelayTrackingResource.buildRequest(properties, "03336306").contains("<ws:Expedition>03336306</ws:Expedition>"));

        assertThrows(IllegalArgumentException.class, () -> MondialRelayTrackingResource.buildRequest(properties, "3336306"));
        assertThrows(IllegalArgumentException.class, () -> MondialRelayTrackingResource.buildRequest(properties, "1234567A"));
        assertThrows(IllegalArgumentException.class, () -> MondialRelayTrackingResource.buildRequest(properties, "<x/>1234"));
        assertThrows(IllegalArgumentException.class, () -> MondialRelayTrackingResource.buildRequest(properties, null));
    }

    @Test
    void parseResponse_readsTheStatusAndTheEventsOldestFirstAndIgnoresTheEmptyOnes() throws IOException {
        // Calquée sur la réponse réelle : les événements arrivent suivis d'entrées vides.
        var tracking = MondialRelayTrackingResource.parseResponse("""
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                <soap:Body><WSI2_TracingColisDetailleResponse xmlns="http://www.mondialrelay.fr/webservice/">
                <WSI2_TracingColisDetailleResult>
                <STAT>82</STAT>
                <Libelle01>Livraison au point relais</Libelle01>
                <Relais_Libelle>STRASBOURG</Relais_Libelle>
                <Relais_Num>031018</Relais_Num>
                <Libelle02 />
                <Tracing>
                <ret_WSI2_sub_TracingColisDetaille><Libelle>Colis livré avec QRCode sécurisé</Libelle><Date>08/11/26</Date><Heure>09:56</Heure><Emplacement>STRASBOURG</Emplacement></ret_WSI2_sub_TracingColisDetaille>
                <ret_WSI2_sub_TracingColisDetaille><Libelle>RÉCEPTION DES DONNÉES</Libelle><Date>02/11/26</Date><Heure>18:52</Heure><Emplacement /></ret_WSI2_sub_TracingColisDetaille>
                <ret_WSI2_sub_TracingColisDetaille><Libelle>PRISE EN CHARGE POINT RELAIS</Libelle><Date>04/11/26</Date><Heure>17:35</Heure><Emplacement>METZ</Emplacement></ret_WSI2_sub_TracingColisDetaille>
                <ret_WSI2_sub_TracingColisDetaille><Libelle /><Date /><Heure /><Emplacement /></ret_WSI2_sub_TracingColisDetaille>
                <ret_WSI2_sub_TracingColisDetaille><Libelle /><Date /><Heure /><Emplacement /></ret_WSI2_sub_TracingColisDetaille>
                </Tracing>
                </WSI2_TracingColisDetailleResult>
                </WSI2_TracingColisDetailleResponse></soap:Body></soap:Envelope>
                """);

        assertEquals(82, tracking.stat());
        assertEquals("Livraison au point relais", tracking.message());
        assertEquals(3, tracking.events().size());
        assertEquals("RÉCEPTION DES DONNÉES", tracking.events().get(0).label());
        // 02/11/26 18:52 à Paris (UTC+1 en novembre).
        assertEquals(Instant.parse("2026-11-02T17:52:00Z"), tracking.events().get(0).at());
        assertEquals(Instant.parse("2026-11-04T16:35:00Z"), tracking.events().get(1).at());
        assertEquals("Colis livré avec QRCode sécurisé", tracking.events().get(2).label());
        assertEquals(Instant.parse("2026-11-08T08:56:00Z"), tracking.events().get(2).at());
    }

    @Test
    void parseResponse_readsAStatusWithoutEvents() throws IOException {
        var tracking = MondialRelayTrackingResource.parseResponse("""
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"><soap:Body>
                <WSI2_TracingColisDetailleResponse xmlns="http://www.mondialrelay.fr/webservice/"><WSI2_TracingColisDetailleResult>
                <STAT>95</STAT><Libelle01 /><Relais_Libelle /><Relais_Num /><Libelle02 />
                </WSI2_TracingColisDetailleResult></WSI2_TracingColisDetailleResponse></soap:Body></soap:Envelope>
                """);

        assertEquals(95, tracking.stat());
        assertEquals("", tracking.message());
        assertTrue(tracking.events().isEmpty());
    }

    @Test
    void parseResponse_refusesAnUnreadableResponseOrOneWithADoctype() {
        assertThrows(IOException.class, () -> MondialRelayTrackingResource.parseResponse("pas du XML"));
        assertThrows(IOException.class, () -> MondialRelayTrackingResource.parseResponse("<a><b/></a>"));
        assertThrows(IOException.class, () -> MondialRelayTrackingResource.parseResponse(
                "<!DOCTYPE x [<!ENTITY e SYSTEM \"file:///etc/passwd\">]><a><STAT>&e;</STAT></a>"));
        assertThrows(IOException.class, () -> MondialRelayTrackingResource.parseResponse(
                "<a><STAT>quatre-vingt</STAT></a>"));
        assertThrows(IOException.class, () -> MondialRelayTrackingResource.parseResponse(
                "<a><STAT>81</STAT><ret_WSI2_sub_TracingColisDetaille><Libelle>X</Libelle><Date>hier</Date><Heure>1</Heure></ret_WSI2_sub_TracingColisDetaille></a>"));
    }

}
