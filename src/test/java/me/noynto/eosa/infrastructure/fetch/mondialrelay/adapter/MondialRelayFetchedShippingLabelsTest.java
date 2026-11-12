package me.noynto.eosa.infrastructure.fetch.mondialrelay.adapter;

import me.noynto.eosa.infrastructure.fetch.mondialrelay.resource.MondialRelayTrackingResource.Event;
import me.noynto.eosa.infrastructure.fetch.mondialrelay.resource.MondialRelayTrackingResource.Tracking;
import me.noynto.eosa.shipping.ShippingLabelStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MondialRelayFetchedShippingLabelsTest {

    private final Event announcement = new Event("RÉCEPTION DES DONNÉES", Instant.parse("2026-11-02T17:52:00Z"));
    private final Event deposit = new Event("PRISE EN CHARGE POINT RELAIS", Instant.parse("2026-11-04T16:35:00Z"));
    private final Event delivery = new Event("Colis livré avec QRCode sécurisé", Instant.parse("2026-11-08T08:56:00Z"));

    @Test
    void toLabel_aRecordedParcelIsNotDepositedYet() {
        var label = MondialRelayFetchedShippingLabels.toLabel(new Tracking(80, "Colis enregistré", List.of(announcement)), "03336306").orElseThrow();

        assertEquals(ShippingLabelStatus.CREATED, label.getStatus());
        assertEquals("03336306", label.getTrackingNumber());
        assertNull(label.getDepositedAt());
        assertNull(label.getDeliveredAt());
    }

    @Test
    void toLabel_aParcelInTheNetworkIsDepositedAtItsFirstEventAfterTheAnnouncement() {
        var label = MondialRelayFetchedShippingLabels.toLabel(
                new Tracking(81, "En cours", List.of(announcement, deposit)), "03336306").orElseThrow();

        assertEquals(ShippingLabelStatus.DEPOSITED, label.getStatus());
        assertEquals(deposit.at(), label.getDepositedAt());
        assertNull(label.getDeliveredAt());
    }

    @Test
    void toLabel_aDeliveredParcelKeepsItsDepositAndDeliveryDates() {
        var label = MondialRelayFetchedShippingLabels.toLabel(
                new Tracking(82, "Livraison au point relais", List.of(announcement, deposit, delivery)), "03336306").orElseThrow();

        assertEquals(ShippingLabelStatus.DELIVERED, label.getStatus());
        assertEquals(deposit.at(), label.getDepositedAt());
        assertEquals(delivery.at(), label.getDeliveredAt());
    }

    @Test
    void toLabel_recognisesTheAnnouncementWhateverTheAccentsAndCase() {
        var shouting = new Event("Reception des donnees", Instant.parse("2026-11-02T17:52:00Z"));

        var label = MondialRelayFetchedShippingLabels.toLabel(new Tracking(81, "", List.of(shouting, deposit)), "03336306").orElseThrow();

        assertEquals(deposit.at(), label.getDepositedAt());
    }

    @Test
    void toLabel_anUnknownParcelIsEmpty() {
        assertTrue(MondialRelayFetchedShippingLabels.toLabel(new Tracking(94, "", List.of()), "03336306").isEmpty());
    }

    @Test
    void toLabel_anAnomalyOrARefusalFailsWithAReadableMessage() {
        var anomaly = assertThrows(RuntimeException.class,
                () -> MondialRelayFetchedShippingLabels.toLabel(new Tracking(83, "Colis endommagé", List.of()), "03336306"));
        assertTrue(anomaly.getMessage().contains("anomalie"));
        assertTrue(anomaly.getMessage().contains("03336306"));
        assertTrue(anomaly.getMessage().contains("Colis endommagé"));

        var refusal = assertThrows(RuntimeException.class,
                () -> MondialRelayFetchedShippingLabels.toLabel(new Tracking(95, "", List.of()), "03336306"));
        assertTrue(refusal.getMessage().contains("95"));
    }

}
