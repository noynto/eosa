package me.noynto.eosa.application;

import me.noynto.eosa.hash.CryptProvider;
import me.noynto.eosa.hash.Hash;
import me.noynto.eosa.hash.Plain;
import me.noynto.eosa.identity.IdentityProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateAdministratorIdentityTest {

    @Mock IdentityProvider identityProvider;
    @Mock CryptProvider cryptProvider;

    @Test
    void handle_writesIdentityWithHashedSecretAndAdminFlag() {
        when(cryptProvider.hash(new Plain("secret"))).thenReturn(new Hash("hashed"));
        when(identityProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = new CreateAdministratorIdentity(identityProvider, cryptProvider)
                .handle(new CreateAdministratorIdentity.Command("admin", "secret"));

        verify(identityProvider).write(argThat(i ->
                "admin".equals(i.getName()) &&
                "hashed".equals(i.getSecret()) &&
                i.isAdministrator()
        ));
        assertEquals("admin", result.getName());
        assertTrue(result.isAdministrator());
    }

    @Test
    void handle_usesHashedPasswordNotPlaintext() {
        when(cryptProvider.hash(new Plain("secret"))).thenReturn(new Hash("bcrypt$hashed"));
        when(identityProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = new CreateAdministratorIdentity(identityProvider, cryptProvider)
                .handle(new CreateAdministratorIdentity.Command("admin", "secret"));

        assertEquals("bcrypt$hashed", result.getSecret());
    }

}