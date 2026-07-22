package me.noynto.eosa.application;

import me.noynto.eosa.cart.Cart;
import me.noynto.eosa.cart.CartItem;
import me.noynto.eosa.cart.CartProvider;
import me.noynto.eosa.cart.CartShippingRule;
import me.noynto.eosa.cart.CartShippingRuleProvider;
import me.noynto.eosa.charm.Charm;
import me.noynto.eosa.charm.CharmProvider;
import me.noynto.eosa.option.Option;
import me.noynto.eosa.option.OptionProvider;
import me.noynto.eosa.option.OptionValue;
import me.noynto.eosa.product.Product;
import me.noynto.eosa.product.ProductProvider;
import me.noynto.eosa.product.Variant;
import me.noynto.eosa.shared.CartId;
import me.noynto.eosa.shared.CharmId;
import me.noynto.eosa.shared.ImageId;
import me.noynto.eosa.shared.OptionId;
import me.noynto.eosa.shared.OptionValueId;
import me.noynto.eosa.shared.ProductId;
import me.noynto.eosa.shared.VariantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddVariantToCartTest {

    @Mock CartProvider cartProvider;
    @Mock ProductProvider productProvider;
    @Mock OptionProvider optionProvider;
    @Mock CharmProvider charmProvider;
    @Mock CartShippingRuleProvider shippingRuleProvider;

    @BeforeEach
    void setUp() {
        lenient().when(shippingRuleProvider.get()).thenReturn(defaultRule());
    }

    @Test
    void handle_addsNewItemWithComposedName() {
        var cartId = new CartId("cart1");
        var productId = new ProductId("prod1");
        var variantId = new VariantId("var1");
        var colorId = new OptionId("color");
        var goldId = new OptionValueId("gold");

        when(cartProvider.read(cartId)).thenReturn(Optional.of(emptyCart(cartId)));

        var product = new Product();
        product.setId(productId);
        product.setName("Pauline");
        product.setOptionIds(List.of(colorId));

        var variant = new Variant();
        variant.setId(variantId);
        variant.setPrice(new BigDecimal("29.90"));
        variant.setOptionValues(Map.of(colorId, goldId));
        variant.setImageIds(List.of(new ImageId("img1")));
        product.setVariants(List.of(variant));

        when(productProvider.read(productId)).thenReturn(Optional.of(product));

        var colorOption = new Option();
        var goldValue = new OptionValue();
        goldValue.setId(goldId);
        goldValue.setLabel("Doré");
        colorOption.setValues(List.of(goldValue));
        when(optionProvider.read(colorId)).thenReturn(Optional.of(colorOption));

        when(cartProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        new AddVariantToCart(cartProvider, productProvider, optionProvider, charmProvider, shippingRuleProvider).handle(
                new AddVariantToCart.Command(cartId, productId, variantId)
        );

        verify(cartProvider).write(argThat(c ->
                c.getItems().size() == 1 &&
                "Pauline — Doré".equals(c.getItems().getFirst().name()) &&
                new BigDecimal("29.90").equals(c.getItems().getFirst().price()) &&
                c.getItems().getFirst().imageId().value().equals("img1")
        ));
    }

    @Test
    void handle_addsItemWithCharmAdditionalPrice() {
        var cartId = new CartId("cart1");
        var productId = new ProductId("prod1");
        var variantId = new VariantId("var1");
        var charmId = new CharmId("charm1");

        when(cartProvider.read(cartId)).thenReturn(Optional.of(emptyCart(cartId)));

        var product = new Product();
        product.setId(productId);
        product.setName("Pauline");
        var variant = new Variant();
        variant.setId(variantId);
        variant.setPrice(new BigDecimal("29.90"));
        product.setVariants(List.of(variant));
        when(productProvider.read(productId)).thenReturn(Optional.of(product));

        var charm = new Charm();
        charm.setId(charmId);
        charm.setAdditionalPrice(new BigDecimal("5.00"));
        when(charmProvider.read(charmId)).thenReturn(Optional.of(charm));

        when(cartProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        new AddVariantToCart(cartProvider, productProvider, optionProvider, charmProvider, shippingRuleProvider).handle(
                new AddVariantToCart.Command(cartId, productId, variantId, charmId)
        );

        verify(cartProvider).write(argThat(c ->
                new BigDecimal("5.00").equals(c.getItems().getFirst().charmAdditionalPrice())
        ));
    }

    @Test
    void handle_incrementsQuantityWhenSameVariantAndCharmAlreadyInCart() {
        var cartId = new CartId("cart1");
        var productId = new ProductId("prod1");
        var variantId = new VariantId("var1");

        var cart = new Cart();
        cart.setId(cartId);
        cart.setItems(List.of(new CartItem(variantId, null, "Pauline", new BigDecimal("29.90"), null, null, 1)));
        when(cartProvider.read(cartId)).thenReturn(Optional.of(cart));

        var product = new Product();
        product.setId(productId);
        product.setName("Pauline");
        var variant = new Variant();
        variant.setId(variantId);
        variant.setPrice(new BigDecimal("29.90"));
        product.setVariants(List.of(variant));
        when(productProvider.read(productId)).thenReturn(Optional.of(product));

        when(cartProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        new AddVariantToCart(cartProvider, productProvider, optionProvider, charmProvider, shippingRuleProvider).handle(
                new AddVariantToCart.Command(cartId, productId, variantId)
        );

        verify(cartProvider).write(argThat(c ->
                c.getItems().size() == 1 && c.getItems().getFirst().quantity() == 2
        ));
    }

    @Test
    void handle_throwsWhenVariantNotFoundOnProduct() {
        var cartId = new CartId("cart1");
        var productId = new ProductId("prod1");
        when(cartProvider.read(cartId)).thenReturn(Optional.of(emptyCart(cartId)));
        var product = new Product();
        product.setId(productId);
        product.setVariants(List.of());
        when(productProvider.read(productId)).thenReturn(Optional.of(product));

        assertThrows(RuntimeException.class, () ->
                new AddVariantToCart(cartProvider, productProvider, optionProvider, charmProvider, shippingRuleProvider).handle(
                        new AddVariantToCart.Command(cartId, productId, new VariantId("unknown"))
                )
        );
    }

    @Test
    void handle_throwsWhenCharmNotFound() {
        var cartId = new CartId("cart1");
        var productId = new ProductId("prod1");
        var variantId = new VariantId("var1");
        when(cartProvider.read(cartId)).thenReturn(Optional.of(emptyCart(cartId)));
        var product = new Product();
        product.setId(productId);
        var variant = new Variant();
        variant.setId(variantId);
        product.setVariants(List.of(variant));
        when(productProvider.read(productId)).thenReturn(Optional.of(product));
        when(charmProvider.read(any())).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                new AddVariantToCart(cartProvider, productProvider, optionProvider, charmProvider, shippingRuleProvider).handle(
                        new AddVariantToCart.Command(cartId, productId, variantId, new CharmId("unknown"))
                )
        );
    }

    @Test
    void handle_throwsWhenCartNotFound() {
        var cartId = new CartId("unknown");
        when(cartProvider.read(cartId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                new AddVariantToCart(cartProvider, productProvider, optionProvider, charmProvider, shippingRuleProvider).handle(
                        new AddVariantToCart.Command(cartId, new ProductId("prod1"), new VariantId("var1"))
                )
        );
    }

    private Cart emptyCart(CartId cartId) {
        var cart = new Cart();
        cart.setId(cartId);
        return cart;
    }

    private CartShippingRule defaultRule() {
        var rule = new CartShippingRule();
        rule.setFreeThreshold(new BigDecimal("60"));
        rule.setAmount(new BigDecimal("5"));
        return rule;
    }

}
