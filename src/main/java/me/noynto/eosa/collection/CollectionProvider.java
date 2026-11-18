package me.noynto.eosa.collection;

import me.noynto.eosa.shared.CollectionId;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public interface CollectionProvider {

    /**
     * Identifiants des collections dans les états demandés ({@code null} ou vide : tous), les plus récemment publiées
     * d'abord ; les brouillons (jamais publiés) en tête, pour l'administration.
     */
    Stream<CollectionId> readIds(Set<CollectionState> states);

    Optional<Collection> read(CollectionId collectionId);

    Collection write(Collection collection);

    void delete(CollectionId collectionId);

}
