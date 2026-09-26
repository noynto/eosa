package me.noynto.eosa.application;

import me.noynto.eosa.identity.Identity;
import me.noynto.eosa.identity.IdentityProvider;
import me.noynto.eosa.image.ImageProvider;
import me.noynto.eosa.jewel.Jewel;
import me.noynto.eosa.jewel.JewelProvider;
import me.noynto.eosa.shared.IdentityId;
import me.noynto.eosa.shared.ImageId;
import me.noynto.eosa.shared.JewelId;

import java.util.ArrayList;
import java.util.List;

public record RemoveImageFromJewel(
        IdentityProvider identityProvider,
        JewelProvider jewelProvider,
        ImageProvider imageProvider
) {

    public Jewel handle(Command command) {
        // 1. Vérification
        if (command.identityId == null || command.identityId.value() == null || command.identityId.value().isBlank()) {
            throw new InvalidCommand("L'identifiant de l'identité est requis.");
        }
        if (command.jewelId == null || command.jewelId.value() == null) {
            throw new InvalidCommand("L'identifiant du produit duquel retirer l'image est nécessaire.");
        }
        if (command.imageId == null || command.imageId.value() == null) {
            throw new InvalidCommand("L'identifiant de l'image à retirer est nécessaire.");
        }

        // 2. Résolution de l'identité fournie dans la commande.
        Identity identity = this.identityProvider.read(command.identityId)
                .orElseThrow(() -> new UnknownIdentity("Aucune identité ne correspond à l'identifiant " + command.identityId.value() + "."));

        if (!identity.isAdministrator()) {
            throw new NotAuthorized("L'identité " + identity.getName() + " n'est pas autorisée à retirer une image d'un produit.");
        }

        // 3. Résolution du produit
        Jewel jewel = jewelProvider.read(command.jewelId())
                .orElseThrow(() -> new RuntimeException("Le produit " + command.jewelId.value() + " duquel retirer l'image n'existe pas."));

        // 4. Retrait de l'image de la liste du produit
        List<ImageId> imageIds = new ArrayList<>(jewel.getImageIds());
        if (!imageIds.remove(command.imageId)) {
            throw new RuntimeException("L'image " + command.imageId.value() + " n'appartient pas au produit " + command.jewelId.value() + ".");
        }
        jewel.setImageIds(imageIds);

        // 5. Enregistrement du produit avant la suppression de l'image, qui ne doit plus y être rattachée.
        Jewel written = jewelProvider.write(jewel);

        // 6. Suppression de l'image chez le fournisseur.
        imageProvider.delete(command.imageId);

        return written;
    }

    public record Command(
            IdentityId identityId,
            JewelId jewelId,
            ImageId imageId
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
