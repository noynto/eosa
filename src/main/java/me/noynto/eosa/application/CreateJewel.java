package me.noynto.eosa.application;

import me.noynto.eosa.identity.Identity;
import me.noynto.eosa.identity.IdentityProvider;
import me.noynto.eosa.jewel.Jewel;
import me.noynto.eosa.jewel.JewelProvider;
import me.noynto.eosa.jewel.JewelState;
import me.noynto.eosa.shared.IdentityId;

public record CreateJewel(
        IdentityProvider identityProvider,
        JewelProvider jewelProvider
) {

    public Jewel handle(Command command) {
        if (command.identityId == null || command.identityId.value() == null || command.identityId.value().isBlank()) {
            throw new InvalidCommand("L'identifiant de l'identité est requis.");
        }
        if (command.name == null || command.name.isBlank()) {
            throw new InvalidCommand("Le nom du produit est requis.");
        }

        // 2. Résolution de l'identité fournie dans la commande.
        Identity identity = this.identityProvider.read(command.identityId)
                .orElseThrow(() -> new UnknownIdentity("Aucune identité ne correspond à l'identifiant " + command.identityId.value() + "."));

        if (!identity.isAdministrator()) {
            throw new NotAuthorized("L'identité " + identity.getName() + " n'est pas autorisé à créer un produit");
        }

        Jewel jewel = new Jewel();
        jewel.setState(JewelState.DRAFTED);
        jewel.setName(command.name());
        return jewelProvider.write(jewel);
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