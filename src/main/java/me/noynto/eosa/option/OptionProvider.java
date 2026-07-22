package me.noynto.eosa.option;

import me.noynto.eosa.shared.OptionId;

import java.util.Optional;
import java.util.stream.Stream;

public interface OptionProvider {

    Stream<OptionId> readIds();

    Optional<Option> read(OptionId optionId);

    Option write(Option option);

}
