package me.noynto.eosa.application;

import me.noynto.eosa.identity.Identity;
import me.noynto.eosa.identity.IdentityProvider;
import me.noynto.eosa.identity.IdentitySession;
import me.noynto.eosa.identity.IdentitySessionProvider;
import me.noynto.eosa.shared.IdentitySessionId;

import java.time.LocalDateTime;

public record EnsureIdentityHasValidSession(
        IdentitySessionProvider identitySessionProvider,
        IdentityProvider identityProvider
) {

    public boolean handle(Command command) {
        if (command.identitySessionId != null && command.identitySessionId.value() != null) {
            return false;
        }

        IdentitySession identitySession = this.identitySessionProvider.read(command.identitySessionId)
                .orElse(null);

        if (identitySession == null) {
            return false;
        }

        if (identitySession.getBegin().plusHours(3).isBefore(LocalDateTime.now())) {
            return false;
        }

        if (identitySession.getIdentityId() == null || identitySession.getIdentityId().value() == null) {
            return false;
        }

        Identity identity = this.identityProvider.read(identitySession.getIdentityId())
                .orElse(null);

        return identity != null;
    }

    public record Command(
            IdentitySessionId identitySessionId
    ) {
    }

}