package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadProduct;
import me.noynto.eosa.application.ReadProductIds;
import me.noynto.eosa.product.ProductState;
import me.noynto.eosa.shared.ProductId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record GetProductHandler(ReadProduct readProduct, ReadProductIds readProductIds) implements Handler {

    private static final String THUMB_ACTIVE = "w-16 h-16 bg-bg-soft rounded-xl border-2 border-primary relative overflow-hidden flex-shrink-0 cursor-pointer";
    private static final String THUMB_INACTIVE = "w-16 h-16 bg-bg-soft rounded-xl relative overflow-hidden flex-shrink-0 cursor-pointer";

    @Override
    public void handle(Context ctx) throws Exception {
        try {
            var productId = new ProductId(ctx.pathParam("id"));
            var product = readProduct.handle(new ReadProduct.Command(productId));
            var allIds = new ArrayList<>(readProductIds.handle(new ReadProductIds.Query(Set.of(ProductState.PUBLISHED), Set.of())));
            allIds.remove(productId);
            Collections.shuffle(allIds);
            var relatedIds = allIds.stream().limit(4).toList();

            boolean hasImages = !product.getImageIds().isEmpty();
            List<Map<String, Object>> images = new ArrayList<>();
            for (int i = 0; i < product.getImageIds().size(); i++) {
                Map<String, Object> image = new HashMap<>();
                image.put("id", product.getImageIds().get(i).value());
                image.put("thumbClass", i == 0 ? THUMB_ACTIVE : THUMB_INACTIVE);
                images.add(image);
            }

            Map<String, Object> model = new HashMap<>();
            model.put("title", "Eosa — " + product.getName());
            model.put("name", product.getName());
            model.put("tagline", product.getTagline() != null ? product.getTagline() : "");
            model.put("price", product.getPrice().stripTrailingZeros().toPlainString());
            model.put("productId", product.getId().value());
            model.put("hasImages", hasImages);
            model.put("mainImageId", hasImages ? product.getImageIds().getFirst().value() : "");
            model.put("images", images);
            model.put("hasRelated", !relatedIds.isEmpty());
            model.put("relatedIds", relatedIds.stream().map(id -> Map.of("id", id.value())).toList());
            ctx.render("product.mustache", model);
        } catch (RuntimeException e) {
            ctx.status(404);
        }
    }

}