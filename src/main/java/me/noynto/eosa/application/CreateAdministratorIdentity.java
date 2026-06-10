package me.noynto.eosa.application;

import me.noynto.eosa.hash.CryptProvider;
import me.noynto.eosa.hash.Plain;
import me.noynto.eosa.identity.Identity;
import me.noynto.eosa.identity.IdentityProvider;

import java.util.Objects;

public record CreateAdministratorIdentity(
        IdentityProvider identityProvider,
        CryptProvider cryptProvider
) {

    public Identity handle(Command command) {
        if (command.name == null || command.name.isBlank()) {
            throw new InvalidCommand("Le nom de l'administrateur est requis.");
        }
        if (command.secret == null || command.secret.isBlank()) {
            throw new InvalidCommand("Le secret de l'administrateur est requis.");
        }
        boolean identityNameAlreadyUsed = this.identityProvider
                .readIds(null)
                .flatMap(identityId -> this.identityProvider.read(identityId).stream())
                .anyMatch(identity -> identity.getName() != null && Objects.deepEquals(identity.getName().toUpperCase(), command.name.toUpperCase()));
        if (identityNameAlreadyUsed) {
            throw new AlreadyUsedName("Le nom de l'identité est déjà utilisé.");
        }
        Identity identity = new Identity();
        identity.setName(command.name());
        identity.setSecret(cryptProvider.hash(new Plain(command.secret())).value());
        identity.setAdministrator(true);
        return this.identityProvider.write(identity);
    }

    public record Command(
            String name,
            String secret
    ) {

    }

    public static class InvalidCommand extends RuntimeException {
        public InvalidCommand(String message) {
            super(message);
        }
    }

    public static class AlreadyUsedName extends RuntimeException {
        public AlreadyUsedName(String message) {
            super(message);
        }
    }



}
