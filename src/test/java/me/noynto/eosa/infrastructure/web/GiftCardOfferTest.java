package me.noynto.eosa.infrastructure.web;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GiftCardOfferTest {

    private GiftCardOffer offer(int validityDays) {
        return new GiftCardOffer(List.of(25, 50, 100), new BigDecimal("10"), new BigDecimal("200"), validityDays);
    }

    @Test
    void validityLabel_readsInYearsWhenItIsWholeAndInDaysOtherwise() {
        assertEquals("un an", offer(365).validityLabel());
        assertEquals("2 ans", offer(730).validityLabel());
        assertEquals("90 jours", offer(90).validityLabel());
    }

    @Test
    void textModel_carriesTheBoundsAndTheValidityForTheTextPages() {
        var model = offer(365).textModel();

        assertEquals(10, model.get("giftCardMinimum"));
        assertEquals(200, model.get("giftCardMaximum"));
        assertEquals("un an", model.get("giftCardValidity"));
    }

}
