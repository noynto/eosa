package me.noynto.eosa.application;

import me.noynto.eosa.charm.Charm;
import me.noynto.eosa.charm.CharmProvider;

import java.util.List;

public record ReadCharms(
        CharmProvider charmProvider
) {

    public List<Charm> handle() {
        return charmProvider.readIds()
                .map(id -> charmProvider.read(id).orElseThrow())
                .toList();
    }

}
