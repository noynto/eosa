package me.noynto.eosa.application;

import me.noynto.eosa.promo.PromoCode;
import me.noynto.eosa.promo.PromoCodeProvider;
import me.noynto.eosa.promo.PromoCodeType;
import me.noynto.eosa.shared.PromoCodeId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdatePromoCodeTest {

    @Mock PromoCodeProvider promoCodeProvider;

    private final PromoCodeId id = new PromoCodeId("abc");

    private PromoCode existing() {
        var promoCode = new PromoCode();
        promoCode.setId(id);
        promoCode.setCode("BIENVENUE10");
        promoCode.setType(PromoCodeType.PERCENT);
        promoCode.setValue(new BigDecimal("10.00"));
        promoCode.setMaxUses(5);
        promoCode.setStartsAt(Instant.parse("2026-10-31T23:00:00Z"));
        return promoCode;
    }

    @Test
    void handle_replacesTheRulesAndKeepsTheCode() {
        when(promoCodeProvider.read(id)).thenReturn(Optional.of(existing()));
        when(promoCodeProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        PromoCode updated = new UpdatePromoCode(promoCodeProvider).handle(new UpdatePromoCode.Command(id,
                PromoCodeType.AMOUNT, new BigDecimal("15"), null, LocalDate.of(2026, 12, 31), null, new BigDecimal("80"))).promoCode();

        assertEquals("BIENVENUE10", updated.getCode());
        assertEquals(PromoCodeType.AMOUNT, updated.getType());
        assertEquals(new BigDecimal("15.00"), updated.getValue());
        assertNull(updated.getStartsAt());
        assertEquals(Instant.parse("2026-12-31T23:00:00Z"), updated.getEndsAt());
        assertNull(updated.getMaxUses());
        assertEquals(new BigDecimal("80.00"), updated.getMinimumAmount());
    }

    @Test
    void handle_refusesInvalidRulesWithoutWriting() {
        assertThrows(RuntimeException.class, () -> new UpdatePromoCode(promoCodeProvider).handle(new UpdatePromoCode.Command(id,
                PromoCodeType.PERCENT, new BigDecimal("150"), null, null, null, null)));
        assertThrows(RuntimeException.class, () -> new UpdatePromoCode(promoCodeProvider).handle(new UpdatePromoCode.Command(id,
                PromoCodeType.PERCENT, new BigDecimal("10"), LocalDate.of(2026, 12, 1), LocalDate.of(2026, 11, 1), null, null)));
        assertThrows(RuntimeException.class, () -> new UpdatePromoCode(promoCodeProvider).handle(new UpdatePromoCode.Command(id,
                null, new BigDecimal("10"), null, null, null, null)));
        verify(promoCodeProvider, never()).write(any());
    }

    @Test
    void handle_throwsWhenIdIsNullOrCodeNotFound() {
        assertThrows(RuntimeException.class, () -> new UpdatePromoCode(promoCodeProvider).handle(new UpdatePromoCode.Command(null,
                PromoCodeType.PERCENT, new BigDecimal("10"), null, null, null, null)));
        when(promoCodeProvider.read(id)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> new UpdatePromoCode(promoCodeProvider).handle(new UpdatePromoCode.Command(id,
                PromoCodeType.PERCENT, new BigDecimal("10"), null, null, null, null)));
    }

}
