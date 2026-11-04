package me.noynto.eosa.application;

import me.noynto.eosa.metal.MetalColor;
import me.noynto.eosa.metal.MetalColorProvider;

public record CreateMetalColor(
        MetalColorProvider metalColorProvider
) {

    public Result handle(Command command) {
        if (command.name == null || command.name.isBlank()) {
            throw new InvalidCommand("Le nom de la couleur de métal est requis.");
        }

        MetalColor metalColor = new MetalColor();
        metalColor.setName(command.name());
        return new Result(metalColorProvider.write(metalColor));
    }

    public record Result(
            MetalColor metalColor
    ) {
    }

    public record Command(
            String name
    ) {
    }

    public static class InvalidCommand extends RuntimeException {
        public InvalidCommand(String message) {
            super(message);
        }
    }

}
