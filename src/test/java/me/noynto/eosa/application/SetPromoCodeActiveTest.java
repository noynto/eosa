package me.noynto.eosa.application;

import me.noynto.eosa.promo.PromoCode;
import me.noynto.eosa.promo.PromoCodeProvider;
import me.noynto.eosa.shared.PromoCodeId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SetPromoCodeActiveTest {

    @Mock PromoCodeProvider promoCodeProvider;

    private final PromoCodeId id = new PromoCodeId("abc");

    @Test
    void handle_deactivatesThenReactivates() {
        var promoCode = new PromoCode();
        promoCode.setId(id);
        when(promoCodeProvider.read(id)).thenReturn(Optional.of(promoCode));
        when(promoCodeProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));
        var useCase = new SetPromoCodeActive(promoCodeProvider);

        assertFalse(useCase.handle(new SetPromoCodeActive.Command(id, false)).promoCode().isActive());
        assertTrue(useCase.handle(new SetPromoCodeActive.Command(id, true)).promoCode().isActive());
    }

    @Test
    void handle_throwsWhenIdIsNullOrCodeNotFound() {
        var useCase = new SetPromoCodeActive(promoCodeProvider);
        assertThrows(RuntimeException.class, () -> useCase.handle(new SetPromoCodeActive.Command(null, true)));
        when(promoCodeProvider.read(id)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> useCase.handle(new SetPromoCodeActive.Command(id, true)));
    }

}
