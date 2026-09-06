package me.noynto.eosa.application;

import me.noynto.eosa.charm.Charm;
import me.noynto.eosa.charm.CharmProvider;

import java.math.BigDecimal;

public record CreateCharm(
        CharmProvider charmProvider
) {

    public Charm handle(Command command) {
        if (command.name == null || command.name.isBlank()) {
            throw new InvalidCommand("Le nom de la breloque est requis.");
        }
        if (command.price == null || command.price.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidCommand("Le prix de la breloque est requis et ne peut pas être négatif.");
        }

        Charm charm = new Charm();
        charm.setName(command.name());
        charm.setPrice(command.price());
        return charmProvider.write(charm);
    }

    public record Command(
            String name,
            BigDecimal price
    ) {
    }

    public static class InvalidCommand extends RuntimeException {
        public InvalidCommand(String message) {
            super(message);
        }
    }

}
