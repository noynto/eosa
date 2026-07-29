package me.noynto.eosa.application;

import me.noynto.eosa.jewel.JewelCategory;
import me.noynto.eosa.jewel.JewelProvider;
import me.noynto.eosa.jewel.JewelState;
import me.noynto.eosa.shared.JewelId;

import java.util.List;
import java.util.Set;

public record ReadJewelIds(
        JewelProvider jewelProvider
) {

    public List<JewelId> handle(Query query) {
        var stream = jewelProvider.readIds(query.states, query.categories);
        if (query.limit != null) {
            stream = stream.limit(query.limit);
        }
        return stream.toList();
    }

    public record Query(
            Set<JewelState> states,
            Set<JewelCategory> categories,
            Integer limit
    ) {
        public Query(Set<JewelState> states, Set<JewelCategory> categories) {
            this(states, categories, null);
        }
    }

}