package me.noynto.eosa.application;

import me.noynto.eosa.collection.Collection;
import me.noynto.eosa.collection.CollectionProvider;
import me.noynto.eosa.collection.CollectionSeason;
import me.noynto.eosa.collection.CollectionState;

public record CreateCollection(
        CollectionProvider collectionProvider
) {

    public Result handle(Command command) {
        if (command.name == null || command.name.isBlank()) {
            throw new InvalidCommand("Le nom de la collection est requis.");
        }
        if (command.season == null) {
            throw new InvalidCommand("La saison de la collection est requise.");
        }
        if (command.year < Collection.MIN_YEAR || command.year > Collection.MAX_YEAR) {
            throw new InvalidCommand("L'année de la collection doit être comprise entre " + Collection.MIN_YEAR + " et " + Collection.MAX_YEAR + ".");
        }
        boolean taken = collectionProvider.readIds(null)
                .map(id -> collectionProvider.read(id).orElseThrow())
                .anyMatch(other -> other.getSeason() == command.season && other.getYear() == command.year);
        if (taken) {
            throw new InvalidCommand("Une collection existe déjà pour cette saison et cette année.");
        }

        Collection collection = new Collection();
        collection.setState(CollectionState.DRAFTED);
        collection.setName(command.name().trim());
        collection.setSeason(command.season());
        collection.setYear(command.year());
        return new Result(collectionProvider.write(collection));
    }

    public record Result(
            Collection collection
    ) {
    }

    public record Command(
            String name,
            CollectionSeason season,
            int year
    ) {
    }

    public static class InvalidCommand extends RuntimeException {
        public InvalidCommand(String message) {
            super(message);
        }
    }

}
