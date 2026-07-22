package me.noynto.eosa.application;

import me.noynto.eosa.identity.Identity;
import me.noynto.eosa.identity.IdentityProvider;
import me.noynto.eosa.option.Option;
import me.noynto.eosa.option.OptionProvider;
import me.noynto.eosa.shared.IdentityId;
import me.noynto.eosa.shared.OptionId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateOptionTest {

    @Mock IdentityProvider identityProvider;
    @Mock OptionProvider optionProvider;

    @Test
    void handle_writesOptionWithName() {
        var identityId = new IdentityId("admin1");
        when(identityProvider.read(identityId)).thenReturn(Optional.of(adminIdentity(identityId)));
        var expected = optionWith("abc");
        when(optionProvider.write(argThat(o -> "Couleur".equals(o.getName())))).thenReturn(expected);

        var result = new CreateOption(identityProvider, optionProvider).handle(
                new CreateOption.Command(identityId, "Couleur")
        );

        assertEquals(expected, result);
    }

    @Test
    void handle_throwsWhenIdentityNotFound() {
        var identityId = new IdentityId("unknown");
        when(identityProvider.read(identityId)).thenReturn(Optional.empty());

        assertThrows(CreateOption.UnknownIdentity.class, () ->
                new CreateOption(identityProvider, optionProvider).handle(
                        new CreateOption.Command(identityId, "Couleur")
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

        assertThrows(CreateOption.NotAuthorized.class, () ->
                new CreateOption(identityProvider, optionProvider).handle(
                        new CreateOption.Command(identityId, "Couleur")
                )
        );
    }

    @Test
    void handle_throwsWhenNameBlank() {
        assertThrows(CreateOption.InvalidCommand.class, () ->
                new CreateOption(identityProvider, optionProvider).handle(
                        new CreateOption.Command(new IdentityId("admin1"), " ")
                )
        );
    }

    private Identity adminIdentity(IdentityId id) {
        var identity = new Identity();
        identity.setId(id);
        identity.setAdministrator(true);
        return identity;
    }

    private Option optionWith(String id) {
        var option = new Option();
        option.setId(new OptionId(id));
        return option;
    }

}
