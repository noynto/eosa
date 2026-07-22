package me.noynto.eosa.application;

import me.noynto.eosa.option.Option;
import me.noynto.eosa.option.OptionProvider;
import me.noynto.eosa.option.OptionValue;
import me.noynto.eosa.shared.OptionId;
import me.noynto.eosa.shared.OptionValueId;

import java.util.UUID;

public record AddValueToOption(
        OptionProvider optionProvider
) {

    public Option handle(Command command) {
        if (command.optionId == null || command.optionId.value() == null) {
            throw new InvalidCommand("L'identifiant de l'option est requis.");
        }
        if (command.label == null || command.label.isBlank()) {
            throw new InvalidCommand("Le libellé de la valeur est requis.");
        }

        Option option = optionProvider.read(command.optionId)
                .orElseThrow(() -> new UnknownOption("L'option " + command.optionId.value() + " n'existe pas."));

        OptionValue value = new OptionValue();
        value.setId(new OptionValueId(UUID.randomUUID().toString()));
        value.setLabel(command.label());
        value.setDescription(command.description());
        option.getValues().add(value);

        return optionProvider.write(option);
    }

    public record Command(
            OptionId optionId,
            String label,
            String description
    ) {
    }

    public static class InvalidCommand extends RuntimeException {
        public InvalidCommand(String message) {
            super(message);
        }
    }

    public static class UnknownOption extends RuntimeException {
        public UnknownOption(String message) {
            super(message);
        }
    }

}
