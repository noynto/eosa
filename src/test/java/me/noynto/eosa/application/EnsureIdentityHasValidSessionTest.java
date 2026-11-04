package me.noynto.eosa.application;

import me.noynto.eosa.identity.Identity;
import me.noynto.eosa.identity.IdentityProvider;
import me.noynto.eosa.identity.IdentitySession;
import me.noynto.eosa.identity.IdentitySessionProvider;
import me.noynto.eosa.shared.IdentityId;
import me.noynto.eosa.shared.IdentitySessionId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EnsureIdentityHasValidSessionTest {

    @Mock IdentitySessionProvider identitySessionProvider;
    @Mock IdentityProvider identityProvider;

    private final LocalDateTime now = LocalDateTime.of(2026, 11, 10, 10, 0);
    private final Clock clock = Clock.fixed(now.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    private final IdentitySessionId sessionId = new IdentitySessionId("session");
    private final IdentityId identityId = new IdentityId("admin");
    private IdentitySession session;
    private Identity identity;

    @BeforeEach
    void setUp() {
        identity = new Identity();
        identity.setId(identityId);
        session = new IdentitySession();
        session.setId(sessionId);
        session.setIdentityId(identityId);
        session.setBegin(now.minusHours(1));
        when(identitySessionProvider.read(sessionId)).thenReturn(Optional.of(session));
        when(identityProvider.read(identityId)).thenReturn(Optional.of(identity));
    }

    @Test
    void handle_returnsTheIdentityOfAValidSession() {
        var result = ensureIdentityHasValidSession().handle(new EnsureIdentityHasValidSession.Command(sessionId));

        assertSame(identity, result.identity());
    }

    @Test
    void handle_hasNoIdentityWithoutASessionId() {
        assertNull(ensureIdentityHasValidSession().handle(new EnsureIdentityHasValidSession.Command(null)).identity());
        assertNull(ensureIdentityHasValidSession().handle(new EnsureIdentityHasValidSession.Command(new IdentitySessionId(null))).identity());
    }

    @Test
    void handle_hasNoIdentityForAnUnknownSession() {
        var unknown = new IdentitySessionId("unknown");
        when(identitySessionProvider.read(unknown)).thenReturn(Optional.empty());

        assertNull(ensureIdentityHasValidSession().handle(new EnsureIdentityHasValidSession.Command(unknown)).identity());
    }

    @Test
    void handle_hasNoIdentityOnceTheSessionIsOlderThanThreeHours() {
        session.setBegin(now.minusHours(3).minusMinutes(1));

        assertNull(ensureIdentityHasValidSession().handle(new EnsureIdentityHasValidSession.Command(sessionId)).identity());
    }

    @Test
    void handle_keepsTheSessionJustBeforeThreeHours() {
        session.setBegin(now.minusHours(3).plusMinutes(1));

        assertSame(identity, ensureIdentityHasValidSession().handle(new EnsureIdentityHasValidSession.Command(sessionId)).identity());
    }

    @Test
    void handle_hasNoIdentityForASessionWithoutIdentity() {
        session.setIdentityId(null);

        assertNull(ensureIdentityHasValidSession().handle(new EnsureIdentityHasValidSession.Command(sessionId)).identity());
    }

    @Test
    void handle_hasNoIdentityWhenTheIdentityNoLongerExists() {
        when(identityProvider.read(identityId)).thenReturn(Optional.empty());

        assertNull(ensureIdentityHasValidSession().handle(new EnsureIdentityHasValidSession.Command(sessionId)).identity());
    }

    private EnsureIdentityHasValidSession ensureIdentityHasValidSession() {
        return new EnsureIdentityHasValidSession(identitySessionProvider, identityProvider, clock);
    }

}
