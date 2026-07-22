package me.noynto.eosa.application;

import me.noynto.eosa.option.OptionProvider;
import me.noynto.eosa.shared.OptionId;

import java.util.List;

public record ReadOptionIds(
        OptionProvider optionProvider
) {

    public List<OptionId> handle(Query query) {
        var stream = optionProvider.readIds();
        if (query != null && query.limit != null) {
            stream = stream.limit(query.limit);
        }
        return stream.toList();
    }

    public record Query(
            Integer limit
    ) {
        public Query() {
            this(null);
        }
    }

}
