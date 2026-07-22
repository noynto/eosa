package me.noynto.eosa.application;

import me.noynto.eosa.charm.Charm;
import me.noynto.eosa.charm.CharmProvider;
import me.noynto.eosa.charm.CharmState;
import me.noynto.eosa.identity.Identity;
import me.noynto.eosa.identity.IdentityProvider;
import me.noynto.eosa.shared.CharmId;
import me.noynto.eosa.shared.IdentityId;
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
class CreateCharmTest {

    @Mock IdentityProvider identityProvider;
    @Mock CharmProvider charmProvider;

    @Test
    void handle_writesCharmWithName() {
        var identityId = new IdentityId("admin1");
        when(identityProvider.read(identityId)).thenReturn(Optional.of(adminIdentity(identityId)));
        var expected = charmWith("abc");
        when(charmProvider.write(argThat(c -> "Lune".equals(c.getName())))).thenReturn(expected);

        var result = new CreateCharm(identityProvider, charmProvider).handle(
                new CreateCharm.Command(identityId, "Lune")
        );

        assertEquals(expected, result);
    }

    @Test
    void handle_setsStateToDrafted() {
        var identityId = new IdentityId("admin1");
        when(identityProvider.read(identityId)).thenReturn(Optional.of(adminIdentity(identityId)));
        when(charmProvider.write(argThat(c -> true))).thenReturn(charmWith("abc"));

        new CreateCharm(identityProvider, charmProvider).handle(
                new CreateCharm.Command(identityId, "Lune")
        );

        verify(charmProvider).write(argThat(c ->
                "Lune".equals(c.getName()) &&
                CharmState.DRAFTED == c.getState()
        ));
    }

    @Test
    void handle_throwsWhenIdentityNotFound() {
        var identityId = new IdentityId("unknown");
        when(identityProvider.read(identityId)).thenReturn(Optional.empty());

        assertThrows(CreateCharm.UnknownIdentity.class, () ->
                new CreateCharm(identityProvider, charmProvider).handle(
                        new CreateCharm.Command(identityId, "Lune")
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

        assertThrows(CreateCharm.NotAuthorized.class, () ->
                new CreateCharm(identityProvider, charmProvider).handle(
                        new CreateCharm.Command(identityId, "Lune")
                )
        );
    }

    private Identity adminIdentity(IdentityId id) {
        var identity = new Identity();
        identity.setId(id);
        identity.setAdministrator(true);
        return identity;
    }

    private Charm charmWith(String id) {
        var charm = new Charm();
        charm.setId(new CharmId(id));
        return charm;
    }

}
