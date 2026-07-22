package me.noynto.eosa.application;

import me.noynto.eosa.option.OptionProvider;
import me.noynto.eosa.shared.OptionId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadOptionIdsTest {

    @Mock OptionProvider optionProvider;

    @Test
    void handle_returnsAllIds() {
        var a = new OptionId("a");
        var b = new OptionId("b");
        when(optionProvider.readIds()).thenReturn(Stream.of(a, b));

        var result = new ReadOptionIds(optionProvider).handle(new ReadOptionIds.Query());

        assertEquals(List.of(a, b), result);
    }

    @Test
    void handle_appliesLimit() {
        var a = new OptionId("a");
        var b = new OptionId("b");
        when(optionProvider.readIds()).thenReturn(Stream.of(a, b));

        var result = new ReadOptionIds(optionProvider).handle(new ReadOptionIds.Query(1));

        assertEquals(List.of(a), result);
    }

}
