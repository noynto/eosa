package me.noynto.eosa.application;

import me.noynto.eosa.identity.Identity;
import me.noynto.eosa.identity.IdentityProvider;
import me.noynto.eosa.identity.IdentitySession;
import me.noynto.eosa.identity.IdentitySessionProvider;
import me.noynto.eosa.shared.IdentitySessionId;

import java.time.LocalDateTime;
import java.util.Optional;

public record EnsureIdentityHasValidSession(
        IdentitySessionProvider identitySessionProvider,
        IdentityProvider identityProvider
) {

    public Optional<Identity> handle(Command command) {
        if (command.identitySessionId == null || command.identitySessionId.value() == null) {
            return Optional.empty();
        }

        IdentitySession identitySession = this.identitySessionProvider.read(command.identitySessionId)
                .orElse(null);

        if (identitySession == null) {
            return Optional.empty();
        }

        if (identitySession.getBegin().plusHours(3).isBefore(LocalDateTime.now())) {
            return Optional.empty();
        }

        if (identitySession.getIdentityId() == null || identitySession.getIdentityId().value() == null) {
            return Optional.empty();
        }

        return this.identityProvider.read(identitySession.getIdentityId());
    }

    public record Command(IdentitySessionId identitySessionId) {}

}