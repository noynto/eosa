package me.noynto.eosa.application;

import me.noynto.eosa.collection.Collection;
import me.noynto.eosa.collection.CollectionProvider;
import me.noynto.eosa.collection.CollectionState;
import me.noynto.eosa.jewel.Jewel;
import me.noynto.eosa.jewel.JewelProvider;
import me.noynto.eosa.jewel.JewelState;
import me.noynto.eosa.shared.CollectionId;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

/**
 * Publie la collection, qui prend la place du hero de l'accueil, et ses bijoux en brouillon. Rien n'est écrit tant qu'un
 * bijou en brouillon est incomplet : une publication partielle serait difficile à repérer. Un bijou archivé reste archivé
 * (l'administratrice l'a retiré exprès) et un bijou déjà publié ne change pas.
 */
public record PublishCollection(
        CollectionProvider collectionProvider,
        JewelProvider jewelProvider,
        Clock clock
) {

    public Result handle(Command command) {
        if (command.collectionId == null || command.collectionId.value() == null) {
            throw new InvalidCommand("L'identifiant de la collection est requis.");
        }
        Collection collection = collectionProvider.read(command.collectionId)
                .orElseThrow(() -> new InvalidCommand("La collection " + command.collectionId.value() + " n'existe pas."));
        if (collection.getState() == CollectionState.PUBLISHED) {
            throw new InvalidCommand("La collection est déjà publiée.");
        }
        if (collection.getVideoId() == null) {
            throw new InvalidCommand("La collection ne peut pas être publiée, il lui faut une vidéo.");
        }
        if (collection.getPosterImageId() == null) {
            throw new InvalidCommand("La collection ne peut pas être publiée, il lui faut une affiche.");
        }

        List<Jewel> toPublish = new ArrayList<>();
        List<String> incomplete = new ArrayList<>();
        for (var jewelId : collection.getJewelIds()) {
            Jewel jewel = jewelProvider.read(jewelId)
                    .orElseThrow(() -> new InvalidCommand("Le bijou " + jewelId.value() + " n'existe pas."));
            if (jewel.getState() == JewelState.ARCHIVED || jewel.getState() == JewelState.PUBLISHED) {
                continue;
            }
            List<String> missing = new ArrayList<>();
            if (jewel.getName() == null || jewel.getName().isBlank()) missing.add("un nom");
            if (jewel.getCategory() == null) missing.add("une catégorie");
            if (jewel.getTagline() == null) missing.add("une accroche");
            if (jewel.getPrice() == null) missing.add("un prix");
            if (jewel.getImageIds() == null || jewel.getImageIds().isEmpty()) missing.add("au moins une image");
            if (!missing.isEmpty()) {
                String name = jewel.getName() == null || jewel.getName().isBlank() ? jewel.getId().value() : "« " + jewel.getName() + " »";
                incomplete.add(name + " (il lui faut " + String.join(", ", missing) + ")");
            }
            toPublish.add(jewel);
        }
        if (!incomplete.isEmpty()) {
            throw new InvalidCommand("La collection ne peut pas être publiée, des bijoux sont incomplets : " + String.join(" ; ", incomplete) + ".");
        }

        // Les bijoux d'abord : si l'écriture de la collection échoue, republier la collection ne change plus rien aux bijoux.
        var now = clock.instant();
        for (Jewel jewel : toPublish) {
            jewel.setState(JewelState.PUBLISHED);
            if (jewel.getPublishedAt() == null) {
                jewel.setPublishedAt(now);
            }
            jewelProvider.write(jewel);
        }
        collection.setState(CollectionState.PUBLISHED);
        collection.setPublishedAt(now);
        return new Result(collectionProvider.write(collection), toPublish.size());
    }

    /** {@code publishedJewelCount} : les bijoux que cette publication a fait passer de brouillon à publié. */
    public record Result(
            Collection collection,
            int publishedJewelCount
    ) {
    }

    public record Command(
            CollectionId collectionId
    ) {
    }

    public static class InvalidCommand extends RuntimeException {
        public InvalidCommand(String message) {
            super(message);
        }
    }

}
