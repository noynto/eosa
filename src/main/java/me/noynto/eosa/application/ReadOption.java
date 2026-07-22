package me.noynto.eosa.application;

import me.noynto.eosa.option.Option;
import me.noynto.eosa.option.OptionProvider;
import me.noynto.eosa.shared.OptionId;

public record ReadOption(
        OptionProvider optionProvider
) {

    public Option handle(Command command) {
        if (command.optionId == null || command.optionId.value() == null) {
            throw new RuntimeException("L'identifiant de l'option est nécessaire.");
        }
        return optionProvider.read(command.optionId())
                .orElseThrow(() -> new RuntimeException("L'option " + command.optionId.value() + " n'existe pas."));
    }

    public record Command(
            OptionId optionId
    ) {
    }

}
