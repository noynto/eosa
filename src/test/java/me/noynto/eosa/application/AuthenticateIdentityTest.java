package me.noynto.eosa.application;

import me.noynto.eosa.hash.CryptProvider;
import me.noynto.eosa.hash.Hash;
import me.noynto.eosa.hash.Plain;
import me.noynto.eosa.identity.Identity;
import me.noynto.eosa.identity.IdentityProvider;
import me.noynto.eosa.identity.IdentitySessionProvider;
import me.noynto.eosa.shared.IdentityId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthenticateIdentityTest {

    @Mock IdentityProvider identityProvider;
    @Mock IdentitySessionProvider identitySessionProvider;
    @Mock CryptProvider cryptProvider;

    private final IdentityId identityId = new IdentityId("admin");

    @BeforeEach
    void setUp() {
        var identity = new Identity();
        identity.setId(identityId);
        identity.setSecret("hashed");
        identity.setAdministrator(true);
        when(identityProvider.readIds(true, "admin")).thenAnswer(inv -> Stream.of(identityId));
        when(identityProvider.read(identityId)).thenReturn(Optional.of(identity));
        when(cryptProvider.check(new Plain("secret"), new Hash("hashed"))).thenReturn(true);
        when(identitySessionProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void handle_opensASessionForTheRightCredentials() {
        var result = authenticateIdentity().handle(new AuthenticateIdentity.Command("admin", "secret"));

        assertEquals(identityId, result.session().getIdentityId());
        assertEquals(LocalDateTime.of(2026, 11, 10, 10, 0), result.session().getBegin());
    }

    @Test
    void handle_refusesAWrongSecretWithoutOpeningASession() {
        assertThrows(AuthenticateIdentity.InvalidCredentials.class, () ->
                authenticateIdentity().handle(new AuthenticateIdentity.Command("admin", "wrong")));
        verify(identitySessionProvider, never()).write(any());
    }

    @Test
    void handle_refusesAnUnknownName() {
        when(identityProvider.readIds(true, "nobody")).thenAnswer(inv -> Stream.empty());

        assertThrows(AuthenticateIdentity.InvalidCredentials.class, () ->
                authenticateIdentity().handle(new AuthenticateIdentity.Command("nobody", "secret")));
        verify(identitySessionProvider, never()).write(any());
    }

    @Test
    void handle_refusesABlankNameOrSecret() {
        assertThrows(AuthenticateIdentity.InvalidCredentials.class, () ->
                authenticateIdentity().handle(new AuthenticateIdentity.Command(" ", "secret")));
        assertThrows(AuthenticateIdentity.InvalidCredentials.class, () ->
                authenticateIdentity().handle(new AuthenticateIdentity.Command("admin", null)));
    }

    private AuthenticateIdentity authenticateIdentity() {
        return new AuthenticateIdentity(identityProvider, identitySessionProvider, cryptProvider,
                Clock.fixed(Instant.parse("2026-11-10T10:00:00Z"), ZoneOffset.UTC));
    }

}
