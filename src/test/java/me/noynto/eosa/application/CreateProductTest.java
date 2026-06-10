package me.noynto.eosa.application;

import me.noynto.eosa.identity.Identity;
import me.noynto.eosa.identity.IdentityProvider;
import me.noynto.eosa.product.Product;
import me.noynto.eosa.product.ProductProvider;
import me.noynto.eosa.product.ProductState;
import me.noynto.eosa.shared.IdentityId;
import me.noynto.eosa.shared.ProductId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateProductTest {

    @Mock IdentityProvider identityProvider;
    @Mock ProductProvider productProvider;

    @Test
    void handle_writesProductWithName() {
        var identityId = new IdentityId("admin1");
        when(identityProvider.read(identityId)).thenReturn(Optional.of(adminIdentity(identityId)));
        var expected = productWith("abc");
        when(productProvider.write(argThat(p -> "Lune".equals(p.getName())))).thenReturn(expected);

        var result = new CreateProduct(identityProvider, productProvider).handle(
                new CreateProduct.Command(identityId, "Lune")
        );

        assertEquals(expected, result);
    }

    @Test
    void handle_setsStateToDrafted() {
        var identityId = new IdentityId("admin1");
        when(identityProvider.read(identityId)).thenReturn(Optional.of(adminIdentity(identityId)));
        when(productProvider.write(argThat(p -> true))).thenReturn(productWith("abc"));

        new CreateProduct(identityProvider, productProvider).handle(
                new CreateProduct.Command(identityId, "Lune")
        );

        verify(productProvider).write(argThat(p ->
                "Lune".equals(p.getName()) &&
                ProductState.DRAFTED == p.getState()
        ));
    }

    @Test
    void handle_throwsWhenIdentityNotFound() {
        var identityId = new IdentityId("unknown");
        when(identityProvider.read(identityId)).thenReturn(Optional.empty());

        assertThrows(CreateProduct.UnknownIdentity.class, () ->
                new CreateProduct(identityProvider, productProvider).handle(
                        new CreateProduct.Command(identityId, "Lune")
                )
        );
    }

    @Test
    void handle_throwsWhenNotAdministrator() {
        var identityId = new IdentityId("user1");
        var identity = new Identity();
        identity.setId(identityId);
        identity.setAdministrator(false);
        when(identityProvider.read(identityId)).thenReturn(Optional.of(identity));

        assertThrows(CreateProduct.NotAuthorized.class, () ->
                new CreateProduct(identityProvider, productProvider).handle(
                        new CreateProduct.Command(identityId, "Lune")
                )
        );
    }

    private Identity adminIdentity(IdentityId id) {
        var identity = new Identity();
        identity.setId(id);
        identity.setAdministrator(true);
        return identity;
    }

    private Product productWith(String id) {
        var product = new Product();
        product.setId(new ProductId(id));
        return product;
    }

}