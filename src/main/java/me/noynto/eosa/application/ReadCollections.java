package me.noynto.eosa.application;

import me.noynto.eosa.collection.Collection;
import me.noynto.eosa.collection.CollectionProvider;
import me.noynto.eosa.collection.CollectionState;

import java.util.List;
import java.util.Set;

public record ReadCollections(
        CollectionProvider collectionProvider
) {

    public Result handle(Command command) {
        return new Result(collectionProvider.readIds(command.states())
                .map(id -> collectionProvider.read(id).orElseThrow())
                .toList());
    }

    public record Result(
            List<Collection> collections
    ) {
    }

    /** {@code states} nul ou vide : toutes les collections. */
    public record Command(
            Set<CollectionState> states
    ) {
    }

}
