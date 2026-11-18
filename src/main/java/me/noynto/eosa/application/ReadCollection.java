package me.noynto.eosa.application;

import me.noynto.eosa.collection.Collection;
import me.noynto.eosa.collection.CollectionProvider;
import me.noynto.eosa.shared.CollectionId;

public record ReadCollection(
        CollectionProvider collectionProvider
) {

    public Result handle(Command command) {
        return new Result(collectionProvider.read(command.collectionId())
                .orElseThrow(() -> new RuntimeException("La collection " + command.collectionId().value() + " n'existe pas.")));
    }

    public record Result(
            Collection collection
    ) {
    }

    public record Command(
            CollectionId collectionId
    ) {
    }

}
