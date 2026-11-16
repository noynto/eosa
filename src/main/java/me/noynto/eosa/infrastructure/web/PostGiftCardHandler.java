package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import me.noynto.eosa.application.PurchaseGiftCard;

import java.math.BigDecimal;

/**
 * Achat d'une carte : crée la carte et la session Stripe, puis envoie la personne payer. Un refus réaffiche la
 * page avec le message et ce qui a été saisi.
 */
public record PostGiftCardHandler(
        PurchaseGiftCard purchaseGiftCard,
        RequestRateLimiter requestRateLimiter,
        GiftCardPurchasePage page
) implements Handler {

    /** Champ caché aux humains (hors écran, sans tabulation) : seuls les robots le remplissent. */
    private static final String HONEYPOT = "website";

    @Override
    public void handle(Context ctx) {
        if (!page.offer().enabled()) {
            ctx.status(404);
            return;
        }
        String honeypot = ctx.formParam(HONEYPOT);
        if (honeypot != null && !honeypot.isBlank()) {
            // Robot : on fait comme si rien ne s'était passé.
            ctx.redirect("/gift-cards", HttpStatus.SEE_OTHER);
            return;
        }
        String preset = ctx.formParam("preset");
        String customAmount = ctx.formParam("amount");
        String firstName = ctx.formParam("firstName");
        String email = ctx.formParam("email");
        if (!requestRateLimiter.tryAcquire(RequestRateLimiter.clientOf(ctx))) {
            page.render(ctx, "Trop de demandes depuis votre connexion. Réessayez dans une heure, "
                    + "ou écrivez-nous sur Instagram (@eosa.bijoux).", preset, customAmount, firstName, email);
            return;
        }
        try {
            // Le montant libre l'emporte sur le bouton choisi quand il est rempli.
            String chosen = customAmount != null && !customAmount.isBlank() ? customAmount : preset;
            BigDecimal amount = PromoCodeForm.decimal(chosen, "Le montant");
            var result = purchaseGiftCard.handle(new PurchaseGiftCard.Command(amount, firstName, email));
            ctx.redirect(result.paymentUri().toString(), HttpStatus.SEE_OTHER);
        } catch (RuntimeException e) {
            page.render(ctx, e.getMessage(), preset, customAmount, firstName, email);
        }
    }

}
