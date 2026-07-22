package me.noynto.eosa.application;

import me.noynto.eosa.option.Option;
import me.noynto.eosa.option.OptionProvider;
import me.noynto.eosa.shared.OptionId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddValueToOptionTest {

    @Mock OptionProvider optionProvider;

    @Test
    void handle_appendsValueToOption() {
        var optionId = new OptionId("abc");
        var option = new Option();
        option.setId(optionId);
        when(optionProvider.read(optionId)).thenReturn(Optional.of(option));
        when(optionProvider.write(argThat(o -> o.getValues().size() == 1
                && "Doré".equals(o.getValues().getFirst().getLabel())
                && "Une chaleur discrète, pensée pour durer".equals(o.getValues().getFirst().getDescription())
        ))).thenReturn(option);

        new AddValueToOption(optionProvider).handle(
                new AddValueToOption.Command(optionId, "Doré", "Une chaleur discrète, pensée pour durer")
        );
    }

    @Test
    void handle_throwsWhenOptionNotFound() {
        var optionId = new OptionId("unknown");
        when(optionProvider.read(optionId)).thenReturn(Optional.empty());

        assertThrows(AddValueToOption.UnknownOption.class, () ->
                new AddValueToOption(optionProvider).handle(
                        new AddValueToOption.Command(optionId, "Doré", null)
                )
        );
    }

    @Test
    void handle_throwsWhenLabelBlank() {
        assertThrows(AddValueToOption.InvalidCommand.class, () ->
                new AddValueToOption(optionProvider).handle(
                        new AddValueToOption.Command(new OptionId("abc"), " ", null)
                )
        );
    }

}
