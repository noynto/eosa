package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.AddImagesToJewel;
import me.noynto.eosa.image.Image;
import me.noynto.eosa.jewel.Jewel;
import me.noynto.eosa.shared.JewelId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record AddImagesToJewelHandler(AddImagesToJewel addImagesToJewel) implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
        var images = ctx.uploadedFiles("images").stream().map(file -> {
            Image image = new Image();
            image.setName(file.filename());
            image.setFormat(file.contentType());
            image.setContent(file.content());
            return image;
        }).toList();
        try {
            Jewel jewel = addImagesToJewel.handle(new AddImagesToJewel.Command(
                    new JewelId(ctx.pathParam("id")),
                    images
            ));
            Map<String, Object> model = new HashMap<>();
            model.put("jewelId", jewel.getId().value());
            model.put("hasImages", !jewel.getImageIds().isEmpty());
            model.put("images", jewel.getImageIds().stream().map(id -> Map.of("id", id.value())).toList());
            ctx.render("admin/partials/jewel-images.mustache", model);
        } catch (RuntimeException e) {
            ctx.status(422).html("<span class=\"text-red-600 text-xs\">" + e.getMessage() + "</span>");
        }
    }

}