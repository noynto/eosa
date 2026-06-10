
@import me.noynto.eosa.checkout.Checkout
@import me.noynto.eosa.checkout.CheckoutStatus
@param Checkout checkout

@template.layouts.main(title = "Eosa — Commande confirmée", content = @`

<section class="px-5 md:px-8 py-20 flex flex-col items-center text-center">
    @if(checkout.getStatus() == CheckoutStatus.COMPLETED)
        <div class="w-16 h-16 rounded-full bg-success flex items-center justify-center mb-6">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.5"><path d="M20 6 9 17l-5-5"/></svg>
        </div>
        <h1 class="font-serif text-4xl font-normal mb-3">Merci pour votre commande !</h1>
        <p class="text-sm text-secondary leading-relaxed max-w-sm mb-8">
            Votre paiement a bien été reçu. Vous allez recevoir un email de confirmation.
        </p>
        @if(checkout.getItems() != null && !checkout.getItems().isEmpty())
            <div class="bg-bg-soft rounded-2xl px-6 py-5 mb-8 text-left w-full max-w-sm">
                <div class="text-xs text-muted mb-3">Récapitulatif</div>
                @for(var item : checkout.getItems())
                    <div class="flex justify-between items-baseline py-2 border-b border-bg-border last:border-0">
                        <span class="text-sm">${item.getName()} × ${item.getQuantity()}</span>
                        <span class="text-sm font-medium">${item.getUnitPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity())).stripTrailingZeros().toPlainString()} €</span>
                    </div>
                @endfor
            </div>
        @endif
    @else
        <div class="w-16 h-16 rounded-full bg-error flex items-center justify-center mb-6">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.5"><path d="M18 6 6 18M6 6l12 12"/></svg>
        </div>
        <h1 class="font-serif text-4xl font-normal mb-3">Paiement non confirmé</h1>
        <p class="text-sm text-secondary leading-relaxed max-w-sm mb-8">
            @if(checkout.getStatus() == CheckoutStatus.EXPIRED)
                Cette session de paiement a expiré.
            @else
                Le paiement n'a pas abouti.
            @endif
        </p>
    @endif
    <a href="/products" class="bg-primary text-accent px-8 py-3.5 rounded-full text-sm font-medium hover:bg-primary-dark transition-colors">
        Continuer mes achats
    </a>
</section>

`)