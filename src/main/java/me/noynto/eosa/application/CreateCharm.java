package me.noynto.eosa.application;

import me.noynto.eosa.charm.Charm;
import me.noynto.eosa.charm.CharmProvider;
import me.noynto.eosa.charm.CharmState;
import me.noynto.eosa.identity.Identity;
import me.noynto.eosa.identity.IdentityProvider;
import me.noynto.eosa.shared.IdentityId;

public record CreateCharm(
        IdentityProvider identityProvider,
        CharmProvider charmProvider
) {

    public Charm handle(Command command) {
        if (command.identityId == null || command.identityId.value() == null || command.identityId.value().isBlank()) {
            throw new InvalidCommand("L'identifiant de l'identité est requis.");
        }
        if (command.name == null || command.name.isBlank()) {
            throw new InvalidCommand("Le nom de la breloque est requis.");
        }

        Identity identity = this.identityProvider.read(command.identityId)
                .orElseThrow(() -> new UnknownIdentity("Aucune identité ne correspond à l'identifiant " + command.identityId.value() + "."));

        if (!identity.isAdministrator()) {
            throw new NotAuthorized("L'identité " + identity.getName() + " n'est pas autorisé à créer une breloque");
        }

        Charm charm = new Charm();
        charm.setState(CharmState.DRAFTED);
        charm.setName(command.name());
        return charmProvider.write(charm);
    }

    public record Command(
            IdentityId identityId,
            String name
    ) {
    }

    public static class InvalidCommand extends RuntimeException {
        public InvalidCommand(String message) {
            super(message);
        }
    }

    public static class UnknownIdentity extends RuntimeException {
        public UnknownIdentity(String message) {
            super(message);
        }
    }

    public static class NotAuthorized extends RuntimeException {
        public NotAuthorized(String message) {
            super(message);
        }
    }

}
