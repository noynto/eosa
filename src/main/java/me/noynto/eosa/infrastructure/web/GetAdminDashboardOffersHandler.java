package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadDashboardOffers;

import java.util.HashMap;
import java.util.Map;

/** Bloc « Offres et catalogue » du tableau de bord : cartes cadeau, codes de promotion, bijoux. */
public record GetAdminDashboardOffersHandler(ReadDashboardOffers readDashboardOffers) implements Handler {

    @Override
    public void handle(Context ctx) {
        var offers = readDashboardOffers.handle();
        var giftCards = offers.giftCards();
        var promoCodes = offers.promoCodes();
        var catalogue = offers.catalogue();

        Map<String, Object> model = new HashMap<>();
        model.put("giftCardsToHonour", OrderFormat.amount(giftCards.toHonour()));
        model.put("giftCardsUsable", giftCards.usable());
        model.put("giftCardsUsableLabel", DashboardFormat.plural(giftCards.usable(), "carte utilisable", "cartes utilisables"));
        model.put("giftCardsIssued", giftCards.issuedThisMonth());
        model.put("giftCardsIssuedLabel", DashboardFormat.plural(giftCards.issuedThisMonth(), "émise ce mois-ci", "émises ce mois-ci"));
        model.put("hasGiftCardExpiry", giftCards.nextExpiry() != null);
        model.put("giftCardExpiry", giftCards.nextExpiry() == null ? "" : DashboardFormat.lastDayBefore(giftCards.nextExpiry()));

        model.put("promoInForce", promoCodes.inForce());
        model.put("promoInForceLabel", DashboardFormat.plural(promoCodes.inForce(), "code en cours", "codes en cours"));
        model.put("promoUses", promoCodes.usesThisMonth());
        model.put("promoUsesLabel", DashboardFormat.plural(promoCodes.usesThisMonth(), "usage", "usages"));
        model.put("promoDiscount", OrderFormat.amount(promoCodes.discountThisMonth()));
        var top = promoCodes.top();
        model.put("hasTopCode", top != null);
        if (top != null) {
            model.put("topCode", top.code());
            model.put("topCodeUses", top.uses() + (top.maxUses() != null ? " sur " + top.maxUses() : "")
                    + DashboardFormat.plural(top.uses(), " usage", " usages"));
            model.put("hasTopCodeEnd", top.endsAt() != null);
            model.put("topCodeEnd", top.endsAt() == null ? "" : DashboardFormat.lastDayBefore(top.endsAt()));
        }

        model.put("published", catalogue.published());
        model.put("publishedLabel", DashboardFormat.plural(catalogue.published(), "publié", "publiés"));
        model.put("necklaces", catalogue.necklaces());
        model.put("necklacesLabel", DashboardFormat.plural(catalogue.necklaces(), "collier", "colliers"));
        model.put("bracelets", catalogue.bracelets());
        model.put("braceletsLabel", DashboardFormat.plural(catalogue.bracelets(), "bracelet", "bracelets"));
        model.put("drafted", catalogue.drafted());
        model.put("draftedLabel", DashboardFormat.plural(catalogue.drafted(), "brouillon", "brouillons"));
        model.put("archived", catalogue.archived());
        model.put("archivedLabel", DashboardFormat.plural(catalogue.archived(), "archivé", "archivés"));
        model.put("hasLastPublished", catalogue.lastPublishedAt() != null);
        model.put("lastPublished", catalogue.lastPublishedAt() == null ? "" : DashboardFormat.shortDay(catalogue.lastPublishedAt()));
        ctx.render("admin/dashboard/offers.mustache", model);
    }

}
