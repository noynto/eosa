package me.noynto.eosa.application;

import me.noynto.eosa.identity.Identity;
import me.noynto.eosa.identity.IdentityProvider;
import me.noynto.eosa.jewel.Jewel;
import me.noynto.eosa.jewel.JewelProvider;
import me.noynto.eosa.jewel.JewelState;
import me.noynto.eosa.shared.IdentityId;
import me.noynto.eosa.shared.JewelId;
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
class CreateJewelTest {

    @Mock IdentityProvider identityProvider;
    @Mock JewelProvider jewelProvider;

    @Test
    void handle_writesJewelWithName() {
        var identityId = new IdentityId("admin1");
        when(identityProvider.read(identityId)).thenReturn(Optional.of(adminIdentity(identityId)));
        var expected = jewelWith("abc");
        when(jewelProvider.write(argThat(p -> "Lune".equals(p.getName())))).thenReturn(expected);

        var result = new CreateJewel(identityProvider, jewelProvider).handle(
                new CreateJewel.Command(identityId, "Lune")
        );

        assertEquals(expected, result);
    }

    @Test
    void handle_setsStateToDrafted() {
        var identityId = new IdentityId("admin1");
        when(identityProvider.read(identityId)).thenReturn(Optional.of(adminIdentity(identityId)));
        when(jewelProvider.write(argThat(p -> true))).thenReturn(jewelWith("abc"));

        new CreateJewel(identityProvider, jewelProvider).handle(
                new CreateJewel.Command(identityId, "Lune")
        );

        verify(jewelProvider).write(argThat(p ->
                "Lune".equals(p.getName()) &&
                JewelState.DRAFTED == p.getState()
        ));
    }

    @Test
    void handle_throwsWhenIdentityNotFound() {
        var identityId = new IdentityId("unknown");
        when(identityProvider.read(identityId)).thenReturn(Optional.empty());

        assertThrows(CreateJewel.UnknownIdentity.class, () ->
                new CreateJewel(identityProvider, jewelProvider).handle(
                        new CreateJewel.Command(identityId, "Lune")
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

        assertThrows(CreateJewel.NotAuthorized.class, () ->
                new CreateJewel(identityProvider, jewelProvider).handle(
                        new CreateJewel.Command(identityId, "Lune")
                )
        );
    }

    private Identity adminIdentity(IdentityId id) {
        var identity = new Identity();
        identity.setId(id);
        identity.setAdministrator(true);
        return identity;
    }

    private Jewel jewelWith(String id) {
        var jewel = new Jewel();
        jewel.setId(new JewelId(id));
        return jewel;
    }

}