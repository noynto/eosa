package me.noynto.eosa.application;

import me.noynto.eosa.charm.CharmProvider;
import me.noynto.eosa.charm.CharmState;
import me.noynto.eosa.shared.CharmId;

import java.util.List;
import java.util.Set;

public record ReadCharmIds(
        CharmProvider charmProvider
) {

    public List<CharmId> handle(Query query) {
        var stream = charmProvider.readIds(query.states);
        if (query.limit != null) {
            stream = stream.limit(query.limit);
        }
        return stream.toList();
    }

    public record Query(
            Set<CharmState> states,
            Integer limit
    ) {
        public Query(Set<CharmState> states) {
            this(states, null);
        }
    }

}
