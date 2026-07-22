package me.noynto.eosa.charm;

import me.noynto.eosa.shared.CharmId;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public interface CharmProvider {

    Stream<CharmId> readIds(Set<CharmState> states);

    Optional<Charm> read(CharmId charmId);

    Charm write(Charm charm);

}
