package me.noynto.eosa.application;

import me.noynto.eosa.option.Option;
import me.noynto.eosa.option.OptionProvider;
import me.noynto.eosa.shared.OptionId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadOptionTest {

    @Mock OptionProvider optionProvider;

    @Test
    void handle_returnsOption() {
        var optionId = new OptionId("abc");
        var option = new Option();
        option.setId(optionId);
        when(optionProvider.read(optionId)).thenReturn(Optional.of(option));

        var result = new ReadOption(optionProvider).handle(new ReadOption.Command(optionId));

        assertEquals(option, result);
    }

    @Test
    void handle_throwsWhenOptionNotFound() {
        var optionId = new OptionId("unknown");
        when(optionProvider.read(optionId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                new ReadOption(optionProvider).handle(new ReadOption.Command(optionId))
        );
    }

}
