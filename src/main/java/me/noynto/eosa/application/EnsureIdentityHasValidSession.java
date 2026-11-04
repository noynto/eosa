package me.noynto.eosa.application;

import me.noynto.eosa.identity.Identity;
import me.noynto.eosa.identity.IdentityProvider;
import me.noynto.eosa.identity.IdentitySession;
import me.noynto.eosa.identity.IdentitySessionProvider;
import me.noynto.eosa.shared.IdentitySessionId;

import java.time.Clock;
import java.time.LocalDateTime;

public record EnsureIdentityHasValidSession(
        IdentitySessionProvider identitySessionProvider,
        IdentityProvider identityProvider,
        Clock clock
) {

    public Result handle(Command command) {
        if (command.identitySessionId == null || command.identitySessionId.value() == null) {
            return new Result(null);
        }

        IdentitySession identitySession = this.identitySessionProvider.read(command.identitySessionId)
                .orElse(null);

        if (identitySession == null) {
            return new Result(null);
        }

        if (identitySession.getBegin().plusHours(3).isBefore(LocalDateTime.now(clock))) {
            return new Result(null);
        }

        if (identitySession.getIdentityId() == null || identitySession.getIdentityId().value() == null) {
            return new Result(null);
        }

        return new Result(this.identityProvider.read(identitySession.getIdentityId()).orElse(null));
    }

    public record Command(IdentitySessionId identitySessionId) {}

    /**
     * @param identity l'identité connectée, ou {@code null} quand la session est absente, expirée ou inconnue :
     *                 c'est le cas normal d'un visiteur sans session, pas une erreur
     */
    public record Result(Identity identity) {}

}