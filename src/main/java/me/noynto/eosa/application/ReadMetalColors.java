package me.noynto.eosa.application;

import me.noynto.eosa.metal.MetalColor;
import me.noynto.eosa.metal.MetalColorProvider;

import java.util.List;

public record ReadMetalColors(
        MetalColorProvider metalColorProvider
) {

    public List<MetalColor> handle() {
        return metalColorProvider.readIds()
                .map(id -> metalColorProvider.read(id).orElseThrow())
                .toList();
    }

}
