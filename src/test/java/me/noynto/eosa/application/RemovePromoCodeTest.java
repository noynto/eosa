package me.noynto.eosa.application;

import me.noynto.eosa.cart.Cart;
import me.noynto.eosa.cart.CartProvider;
import me.noynto.eosa.shared.CartId;
import me.noynto.eosa.shared.PromoCodeId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemovePromoCodeTest {

    @Mock CartProvider cartProvider;

    private final CartId cartId = new CartId("cart");

    @Test
    void handle_detachesTheCode() {
        var cart = new Cart();
        cart.setPromoCodeId(new PromoCodeId("p-1"));
        when(cartProvider.read(cartId)).thenReturn(Optional.of(cart));
        when(cartProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = new RemovePromoCode(cartProvider).handle(new RemovePromoCode.Command(cartId));

        assertNull(result.cart().getPromoCodeId());
        verify(cartProvider).write(cart);
    }

    @Test
    void handle_throwsWhenIdIsNullOrCartNotFound() {
        var useCase = new RemovePromoCode(cartProvider);
        assertThrows(RuntimeException.class, () -> useCase.handle(new RemovePromoCode.Command(null)));
        when(cartProvider.read(cartId)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> useCase.handle(new RemovePromoCode.Command(cartId)));
    }

}
