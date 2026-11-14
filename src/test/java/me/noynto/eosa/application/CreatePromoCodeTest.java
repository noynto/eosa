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
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreatePromoCodeTest {

    @Mock PromoCodeProvider promoCodeProvider;

    private final Instant now = Instant.parse("2026-11-14T10:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);

    private CreatePromoCode useCase() {
        return new CreatePromoCode(promoCodeProvider, clock);
    }

    private CreatePromoCode.Command command(String code, PromoCodeType type, String value) {
        return new CreatePromoCode.Command(code, type, new BigDecimal(value), null, null, null, null);
    }

    @Test
    void handle_createsAnActiveCodeWithoutLimits() {
        when(promoCodeProvider.readIds("BIENVENUE10")).thenReturn(Stream.empty());
        when(promoCodeProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        PromoCode created = useCase().handle(command("BIENVENUE10", PromoCodeType.PERCENT, "10")).promoCode();

        assertEquals("BIENVENUE10", created.getCode());
        assertEquals(PromoCodeType.PERCENT, created.getType());
        assertEquals(new BigDecimal("10.00"), created.getValue());
        assertTrue(created.isActive());
        assertEquals(now, created.getCreatedAt());
        assertNull(created.getStartsAt());
        assertNull(created.getEndsAt());
        assertNull(created.getMaxUses());
        assertNull(created.getMinimumAmount());
    }

    @Test
    void handle_normalizesTheCode() {
        when(promoCodeProvider.readIds("ETE10")).thenReturn(Stream.empty());
        when(promoCodeProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        PromoCode created = useCase().handle(command("  été 10 ", PromoCodeType.PERCENT, "10")).promoCode();

        assertEquals("ETE10", created.getCode());
    }

    @Test
    void handle_acceptsAFixedAmountWithTheFrenchRounding() {
        when(promoCodeProvider.readIds("MERCI")).thenReturn(Stream.empty());
        when(promoCodeProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        PromoCode created = useCase().handle(command("MERCI", PromoCodeType.AMOUNT, "15.5")).promoCode();

        assertEquals(new BigDecimal("15.50"), created.getValue());
    }

    @Test
    void handle_keepsTheLastDayOfValidityIncluded() {
        // Heure de Paris en novembre : UTC+1. Début le 1er à 00:00, fin exclusive le 1er décembre à 00:00.
        when(promoCodeProvider.readIds("NOV")).thenReturn(Stream.empty());
        when(promoCodeProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        PromoCode created = useCase().handle(new CreatePromoCode.Command("NOV", PromoCodeType.PERCENT, new BigDecimal("5"),
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), 50, new BigDecimal("60"))).promoCode();

        assertEquals(Instant.parse("2026-10-31T23:00:00Z"), created.getStartsAt());
        assertEquals(Instant.parse("2026-11-30T23:00:00Z"), created.getEndsAt());
        assertEquals(50, created.getMaxUses());
        assertEquals(new BigDecimal("60.00"), created.getMinimumAmount());
    }

    @Test
    void handle_refusesAnExistingCode() {
        when(promoCodeProvider.readIds("DEJA")).thenReturn(Stream.of(new PromoCodeId("x")));

        var error = assertThrows(RuntimeException.class, () -> useCase().handle(command("deja", PromoCodeType.PERCENT, "10")));

        assertTrue(error.getMessage().contains("DEJA"));
        verify(promoCodeProvider, never()).write(any());
    }

    @Test
    void handle_refusesAnInvalidCode() {
        assertThrows(RuntimeException.class, () -> useCase().handle(command(null, PromoCodeType.PERCENT, "10")));
        assertThrows(RuntimeException.class, () -> useCase().handle(command("  ", PromoCodeType.PERCENT, "10")));
        assertThrows(RuntimeException.class, () -> useCase().handle(command("AB", PromoCodeType.PERCENT, "10")));
        assertThrows(RuntimeException.class, () -> useCase().handle(command("A".repeat(31), PromoCodeType.PERCENT, "10")));
        assertThrows(RuntimeException.class, () -> useCase().handle(command("PROMO!", PromoCodeType.PERCENT, "10")));
        verify(promoCodeProvider, never()).write(any());
    }

    @Test
    void handle_refusesAnInvalidValue() {
        assertThrows(RuntimeException.class, () -> useCase().handle(command("PROMO", null, "10")));
        assertThrows(RuntimeException.class, () -> useCase().handle(command("PROMO", PromoCodeType.PERCENT, "0")));
        assertThrows(RuntimeException.class, () -> useCase().handle(command("PROMO", PromoCodeType.PERCENT, "101")));
        assertThrows(RuntimeException.class, () -> useCase().handle(command("PROMO", PromoCodeType.PERCENT, "12.5")));
        assertThrows(RuntimeException.class, () -> useCase().handle(command("PROMO", PromoCodeType.AMOUNT, "-5")));
        assertThrows(RuntimeException.class, () -> useCase().handle(command("PROMO", PromoCodeType.AMOUNT, "10001")));
        assertThrows(RuntimeException.class, () -> useCase().handle(
                new CreatePromoCode.Command("PROMO", PromoCodeType.PERCENT, null, null, null, null, null)));
        verify(promoCodeProvider, never()).write(any());
    }

    @Test
    void handle_refusesInconsistentLimits() {
        assertThrows(RuntimeException.class, () -> useCase().handle(new CreatePromoCode.Command("PROMO", PromoCodeType.PERCENT,
                new BigDecimal("10"), LocalDate.of(2026, 12, 1), LocalDate.of(2026, 11, 1), null, null)));
        assertThrows(RuntimeException.class, () -> useCase().handle(new CreatePromoCode.Command("PROMO", PromoCodeType.PERCENT,
                new BigDecimal("10"), null, null, 0, null)));
        assertThrows(RuntimeException.class, () -> useCase().handle(new CreatePromoCode.Command("PROMO", PromoCodeType.PERCENT,
                new BigDecimal("10"), null, null, null, BigDecimal.ZERO)));
        verify(promoCodeProvider, never()).write(any());
    }

}
