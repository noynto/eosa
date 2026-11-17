package me.noynto.eosa.infrastructure.web;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PriceFormatTest {

    @Test
    void writesARoundAmountWithoutDecimals() {
        assertEquals("42", PriceFormat.price(new BigDecimal("42")));
        assertEquals("42", PriceFormat.price(new BigDecimal("42.00")));
        assertEquals("0", PriceFormat.price(BigDecimal.ZERO));
        assertEquals("100", PriceFormat.price(new BigDecimal("1E+2")));
    }

    @Test
    void writesTwoDecimalsWithACommaOtherwise() {
        assertEquals("4,90", PriceFormat.price(new BigDecimal("4.9")));
        assertEquals("6,50", PriceFormat.price(new BigDecimal("6.50")));
        assertEquals("0,50", PriceFormat.price(new BigDecimal("0.5")));
        assertEquals("12,35", PriceFormat.price(new BigDecimal("12.345")));
    }

}
