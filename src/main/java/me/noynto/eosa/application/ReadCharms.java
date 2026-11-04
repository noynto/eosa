package me.noynto.eosa.application;

import me.noynto.eosa.charm.Charm;
import me.noynto.eosa.charm.CharmProvider;

import java.util.List;

public record ReadCharms(
        CharmProvider charmProvider
) {

    public Result handle() {
        return new Result(charmProvider.readIds()
                .map(id -> charmProvider.read(id).orElseThrow())
                .toList());
    }

    public record Result(
            List<Charm> charms
    ) {
    }

}
