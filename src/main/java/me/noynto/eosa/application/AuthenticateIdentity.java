package me.noynto.eosa.application;

import me.noynto.eosa.hash.CryptProvider;
import me.noynto.eosa.hash.Hash;
import me.noynto.eosa.hash.Plain;
import me.noynto.eosa.identity.Identity;
import me.noynto.eosa.identity.IdentityProvider;
import me.noynto.eosa.identity.IdentitySession;
import me.noynto.eosa.identity.IdentitySessionProvider;

import java.time.LocalDateTime;

public record AuthenticateIdentity(
        IdentityProvider identityProvider,
        IdentitySessionProvider identitySessionProvider,
        CryptProvider cryptProvider
) {

    public IdentitySession handle(Command command) {
        if (command.name == null || command.name.isBlank()) {
            throw new InvalidCredentials("Identifiants incorrects.");
        }
        if (command.secret == null || command.secret.isBlank()) {
            throw new InvalidCredentials("Identifiants incorrects.");
        }

        Identity identity = identityProvider.readIds(true, command.name)
                .flatMap(id -> identityProvider.read(id).stream())
                .findFirst()
                .orElseThrow(() -> new InvalidCredentials("Identifiants incorrects."));

        if (!cryptProvider.check(new Plain(command.secret), new Hash(identity.getSecret()))) {
            throw new InvalidCredentials("Identifiants incorrects.");
        }

        IdentitySession session = new IdentitySession();
        session.setIdentityId(identity.getId());
        session.setBegin(LocalDateTime.now());
        return identitySessionProvider.write(session);
    }

    public record Command(String name, String secret) {}

    public static class InvalidCredentials extends RuntimeException {
        public InvalidCredentials(String message) { super(message); }
    }

}