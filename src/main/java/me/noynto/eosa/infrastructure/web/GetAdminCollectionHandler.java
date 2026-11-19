package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadCollection;
import me.noynto.eosa.collection.Collection;
import me.noynto.eosa.collection.CollectionState;
import me.noynto.eosa.shared.CollectionId;

import java.util.HashMap;
import java.util.Map;

public record GetAdminCollectionHandler(ReadCollection readCollection) implements Handler {

    @Override
    public void handle(Context ctx) {
        try {
            var collection = readCollection.handle(new ReadCollection.Command(new CollectionId(ctx.pathParam("id")))).collection();
            boolean hasVideo = collection.getVideoId() != null;
            boolean hasPoster = collection.getPosterImageId() != null;
            Map<String, Object> model = new HashMap<>();
            model.put("title", collection.getName());
            model.put("collectionId", collection.getId().value());
            model.put("name", collection.getName());
            model.put("year", collection.getYear());
            model.put("headline", collection.getHeadline() != null ? collection.getHeadline() : "");
            model.put("blurb", collection.getBlurb() != null ? collection.getBlurb() : "");
            model.put("maxHeadlineLength", Collection.MAX_HEADLINE_LENGTH);
            model.put("maxBlurbLength", Collection.MAX_BLURB_LENGTH);
            model.put("seasons", CollectionLabels.seasonOptions(collection.getSeason()));
            model.put("stateLabel", CollectionLabels.state(collection.getState()));
            model.put("published", collection.getState() == CollectionState.PUBLISHED);
            model.put("archived", collection.getState() == CollectionState.ARCHIVED);
            model.put("hasVideo", hasVideo);
            model.put("videoId", hasVideo ? collection.getVideoId().value() : "");
            model.put("hasPoster", hasPoster);
            model.put("posterImageId", hasPoster ? collection.getPosterImageId().value() : "");
            ctx.render("admin/collection.mustache", model);
        } catch (RuntimeException e) {
            ctx.status(404);
        }
    }

}
