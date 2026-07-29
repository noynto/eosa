package me.noynto.eosa.jewel;

import me.noynto.eosa.shared.JewelId;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public interface JewelProvider {

    Stream<JewelId> readIds(Set<JewelState> states, Set<JewelCategory> categories);

    Optional<Jewel> read(JewelId jewelId);

    Jewel write(Jewel jewel);

}
