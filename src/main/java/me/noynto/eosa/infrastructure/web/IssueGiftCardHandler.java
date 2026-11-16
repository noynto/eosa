package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.IssueGiftCardByAdmin;

public record IssueGiftCardHandler(IssueGiftCardByAdmin issueGiftCardByAdmin) implements Handler {

    @Override
    public void handle(Context ctx) {
        try {
            var giftCard = issueGiftCardByAdmin.handle(new IssueGiftCardByAdmin.Command(
                    PromoCodeForm.decimal(ctx.formParam("amount"), "Le montant"),
                    ctx.formParam("recipientFirstName"),
                    ctx.formParam("recipientEmail"),
                    ctx.formParam("reason")
            )).giftCard();
            ctx.header("HX-Redirect", "/gift-cards/" + giftCard.getId().value());
            ctx.status(200);
        } catch (RuntimeException e) {
            InlineError.render(ctx, e);
        }
    }

}
