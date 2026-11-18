package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadCollections;
import me.noynto.eosa.collection.CollectionState;
import me.noynto.eosa.service.BusinessTime;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public record GetAdminCollectionsHandler(ReadCollections readCollections, Clock clock) implements Handler {

    @Override
    public void handle(Context ctx) {
        var collections = readCollections.handle(new ReadCollections.Command(null)).collections();

        var rows = collections.stream().map(collection -> {
            boolean hasPoster = collection.getPosterImageId() != null;
            int jewelCount = collection.getJewelIds().size();
            Map<String, Object> row = new HashMap<>();
            row.put("id", collection.getId().value());
            row.put("name", collection.getName());
            row.put("stateLabel", CollectionLabels.state(collection.getState()));
            row.put("published", collection.getState() == CollectionState.PUBLISHED);
            row.put("archived", collection.getState() == CollectionState.ARCHIVED);
            row.put("hasPoster", hasPoster);
            row.put("posterImageId", hasPoster ? collection.getPosterImageId().value() : "");
            row.put("hasVideo", collection.getVideoId() != null);
            row.put("jewelCountLabel", jewelCount + (jewelCount > 1 ? " bijoux" : " bijou"));
            return row;
        }).toList();

        Map<String, Object> model = new HashMap<>();
        model.put("title", "Eosa — Collections");
        model.put("collections", rows);
        model.put("hasCollections", !rows.isEmpty());
        model.put("seasons", CollectionLabels.seasonOptions(null));
        model.put("year", LocalDate.now(clock.withZone(BusinessTime.ZONE)).getYear());
        ctx.render("admin/collections.mustache", model);
    }

}
