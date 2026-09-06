package me.noynto.eosa.metal;

import me.noynto.eosa.shared.MetalColorId;

import java.util.Optional;
import java.util.stream.Stream;

public interface MetalColorProvider {

    Stream<MetalColorId> readIds();

    Optional<MetalColor> read(MetalColorId metalColorId);

    MetalColor write(MetalColor metalColor);

}
