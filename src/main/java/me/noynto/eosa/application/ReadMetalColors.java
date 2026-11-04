package me.noynto.eosa.application;

import me.noynto.eosa.metal.MetalColor;
import me.noynto.eosa.metal.MetalColorProvider;

import java.util.List;

public record ReadMetalColors(
        MetalColorProvider metalColorProvider
) {

    public Result handle() {
        return new Result(metalColorProvider.readIds()
                .map(id -> metalColorProvider.read(id).orElseThrow())
                .toList());
    }

    public record Result(
            List<MetalColor> metalColors
    ) {
    }

}
