package me.noynto.eosa.infrastructure.fetch.mondialrelay.resource;

import me.noynto.eosa.infrastructure.fetch.mondialrelay.config.MondialRelayConnectProperties;
import me.noynto.eosa.shipping.ShippingLabel;
import me.noynto.eosa.shipping.ShippingRecipient;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MondialRelayShipmentResourceTest {

    private final MondialRelayConnectProperties properties = new MondialRelayConnectProperties(
            URI.create("https://connect-api-sandbox.mondialrelay.com/api/shipment"),
            "BDTEST@business-api.mondialrelay.com", "s3cret&<pass>", "BDTEST",
            new MondialRelayConnectProperties.Sender("EOSA", "1 rue de la Paix", "54000", "Nancy", "+33 3 83 00 00 00", "contact@eosa.me"));

    @Test
    void buildRequest_describesAHomeDeliveryOfOneParcel() {
        var xml = MondialRelayShipmentResource.buildRequest(properties, label());

        assertTrue(xml.contains("<Login>BDTEST@business-api.mondialrelay.com</Login>"));
        assertTrue(xml.contains("<CustomerId>BDTEST</CustomerId>"));
        assertTrue(xml.contains("<OutputFormat>10x15</OutputFormat>"));
        assertTrue(xml.contains("<OutputType>PdfUrl</OutputType>"));
        assertTrue(xml.contains("<DeliveryMode Mode=\"HOM\" Location=\"\" />"));
        assertTrue(xml.contains("<CollectionMode Mode=\"CCC\" Location=\"\" />"));
        assertTrue(xml.contains("<Weight Value=\"250\" Unit=\"gr\" />"));
        // Sans dimensions, le transporteur répond WebApi_MandatoryParcelSize (constaté en bac à sable).
        // Les dimensions sont celles saisies à la fabrication, portées par l'étiquette.
        assertTrue(xml.contains("<Length Value=\"20\" Unit=\"cm\" /><Width Value=\"15\" Unit=\"cm\" /><Depth Value=\"6\" Unit=\"cm\" />"));
        assertTrue(xml.contains("<OrderNo>EOSA-2026-0001</OrderNo>"));
        assertTrue(xml.contains("<PhoneNo>+33383000000</PhoneNo>"));
    }

    @Test
    void buildRequest_escapesTheSecretsAndKeepsTheXmlWellFormed() throws IOException {
        var xml = MondialRelayShipmentResource.buildRequest(properties, label());

        assertTrue(xml.contains("<Password>s3cret&amp;&lt;pass&gt;</Password>"));
        // Le XML produit se relit sans erreur.
        MondialRelayShipmentResource.parseResponse(xml.replace("ShipmentCreationRequest", "ShipmentCreationResponse")
                .replace("<Shipment>", "<Shipment ShipmentNumber=\"1\">").replace("</Parcels>", "</Parcels><Output>https://x</Output>"));
    }

    @Test
    void buildRequest_cleansTheRecipientToTheCarrierFormat() {
        var recipient = new ShippingRecipient("Zoë de la Rivière-Ségur Beauchamp-Lefèvre", "zoe@example.com", "+33 6 00 00 00 00",
                "12 bis, rue de l'Église <Nord> & co. très longue adresse", "Résidence \"Les Œillets\"", "54000", "Saint-Nicolas-de-Port", "FR");
        var label = label();
        label.setRecipient(recipient);

        var xml = MondialRelayShipmentResource.buildRequest(properties, label);

        assertTrue(xml.contains("<AddressAdd1>ZOE DE LA RIVIERE-SEGUR BEAUCH</AddressAdd1>"), xml);
        assertTrue(xml.contains("<Streetname>12 BIS, RUE DE L&apos;EGLISE NORD CO. TRES LO</Streetname>"), xml);
        assertTrue(xml.contains("<AddressAdd3>RESIDENCE LES OEILLETS</AddressAdd3>"), xml);
        assertFalse(xml.contains("<Nord>"));
    }

    @Test
    void clean_removesAccentsAndForbiddenCharactersThenTruncates() {
        assertEquals("ELEVE ECOLE", MondialRelayShipmentResource.clean("Élève  école", 40));
        assertEquals("A B", MondialRelayShipmentResource.clean("a#b", 40));
        assertEquals("ABCD", MondialRelayShipmentResource.clean("abcdefgh", 4));
        assertEquals("", MondialRelayShipmentResource.clean(null, 4));
    }

    @Test
    void parseResponse_readsTheShipmentNumberAndTheLabelAddress() throws IOException {
        var created = MondialRelayShipmentResource.parseResponse("""
                <?xml version="1.0" encoding="utf-16"?>
                <ShipmentCreationResponse xmlns="http://www.example.org/Response">
                  <OutputOptions><OutputFormat>10x15</OutputFormat><OutputType>PdfUrl</OutputType></OutputOptions>
                  <ShipmentsList>
                    <Shipment ShipmentNumber="96408887">
                      <LabelList><Label>
                        <Output>http://connect.mondialrelay.com//BDTEST/etiquette/GetStickersExpeditionsAnonyme?ens=BDTEST&amp;expedition=96408887&amp;crc=ABC</Output>
                      </Label></LabelList>
                    </Shipment>
                  </ShipmentsList>
                  <StatusList />
                </ShipmentCreationResponse>
                """);

        assertEquals("96408887", created.shipmentNumber());
        assertEquals("http://connect.mondialrelay.com//BDTEST/etiquette/GetStickersExpeditionsAnonyme?ens=BDTEST&expedition=96408887&crc=ABC",
                created.labelUrl());
    }

    @Test
    void parseResponse_turnsABusinessErrorIntoAReadableMessage() {
        var error = assertThrows(RuntimeException.class, () -> MondialRelayShipmentResource.parseResponse("""
                <ShipmentCreationResponse>
                  <ShipmentsList />
                  <StatusList><Status Code="10001" Level="Critical Error" Message="Invalid user and/or password" /></StatusList>
                </ShipmentCreationResponse>
                """));

        assertTrue(error.getMessage().contains("Invalid user and/or password"));
        assertTrue(error.getMessage().contains("10001"));
    }

    @Test
    void parseResponse_readsTheSuccessStatusOfTheSandboxAndKeepsTheLeadingZerosOfTheNumber() throws IOException {
        // Réponse réelle du bac à sable : le statut de succès n'a pas d'attribut Level.
        var created = MondialRelayShipmentResource.parseResponse("""
                <ShipmentCreationResponse xmlns="http://www.example.org/Response">
                  <ShipmentsList><Shipment ShipmentNumber="00581106"><LabelList><Label>
                    <Output>https://connect-sandbox.mondialrelay.com//TTMRSDBX/etiquette/GetStickersExpeditionsAnonyme2?ens=TTMRSDBX&amp;expedition=00581106&amp;crc=ABC</Output>
                  </Label></LabelList></Shipment></ShipmentsList>
                  <StatusList><Status Code="0" Message="API Mondial Relay Sandbox, aucune donnée n'a été enregistrée." /></StatusList>
                </ShipmentCreationResponse>
                """);

        assertEquals("00581106", created.shipmentNumber());
    }

    @Test
    void parseResponse_turnsTheMissingParcelSizeIntoAReadableMessage() {
        var error = assertThrows(RuntimeException.class, () -> MondialRelayShipmentResource.parseResponse("""
                <ShipmentCreationResponse>
                  <StatusList><Status Code="10106" Level="Error" Message="WebApi_MandatoryParcelSize" /></StatusList>
                </ShipmentCreationResponse>
                """));

        assertTrue(error.getMessage().contains("WebApi_MandatoryParcelSize"));
    }

    @Test
    void parseResponse_ignoresAWarningWhenTheLabelIsThere() throws IOException {
        var created = MondialRelayShipmentResource.parseResponse("""
                <ShipmentCreationResponse>
                  <ShipmentsList><Shipment ShipmentNumber="42"><LabelList><Label><Output>https://x/42.pdf</Output></Label></LabelList></Shipment></ShipmentsList>
                  <StatusList><Status Code="10014" Level="Warning" Message="Invalid order number. Statement ignored." /></StatusList>
                </ShipmentCreationResponse>
                """);

        assertEquals("42", created.shipmentNumber());
    }

    @Test
    void parseResponse_refusesAResponseWithoutLabelOrWithADoctype() {
        assertThrows(RuntimeException.class, () -> MondialRelayShipmentResource.parseResponse(
                "<ShipmentCreationResponse><ShipmentsList /><StatusList /></ShipmentCreationResponse>"));
        assertThrows(IOException.class, () -> MondialRelayShipmentResource.parseResponse(
                "<!DOCTYPE x [<!ENTITY e SYSTEM \"file:///etc/passwd\">]><ShipmentCreationResponse>&e;</ShipmentCreationResponse>"));
        assertThrows(IOException.class, () -> MondialRelayShipmentResource.parseResponse("pas du XML"));
    }

    private ShippingLabel label() {
        var label = new ShippingLabel();
        label.setOrderNumber("EOSA-2026-0001");
        label.setWeightGrams(250);
        label.setLengthCm(20);
        label.setWidthCm(15);
        label.setDepthCm(6);
        label.setRecipient(new ShippingRecipient("Ada Lovelace", "ada@example.com", "+33600000000",
                "1 rue des Arts", null, "54000", "Nancy", "FR"));
        return label;
    }

}
