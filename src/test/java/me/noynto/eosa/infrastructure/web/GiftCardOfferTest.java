package me.noynto.eosa.infrastructure.web;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GiftCardOfferTest {

    private GiftCardOffer offer(boolean enabled, int validityDays) {
        return new GiftCardOffer(enabled, List.of(25, 50, 100), new BigDecimal("10"), new BigDecimal("200"), validityDays);
    }

    @Test
    void validityLabel_readsInYearsWhenItIsWholeAndInDaysOtherwise() {
        assertEquals("un an", offer(true, 365).validityLabel());
        assertEquals("2 ans", offer(true, 730).validityLabel());
        assertEquals("90 jours", offer(true, 90).validityLabel());
    }

    @Test
    void textModel_carriesTheSwitchTheBoundsAndTheValidityForTheTextPages() {
        var closed = offer(false, 365).textModel();
        var open = offer(true, 365).textModel();

        assertFalse((Boolean) closed.get("giftCardsEnabled"));
        assertTrue((Boolean) open.get("giftCardsEnabled"));
        assertEquals(10, open.get("giftCardMinimum"));
        assertEquals(200, open.get("giftCardMaximum"));
        assertEquals("un an", open.get("giftCardValidity"));
    }

}
