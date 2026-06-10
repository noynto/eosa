@import me.noynto.eosa.cart.Cart
@import me.noynto.eosa.cart.CartItem
@import java.math.BigDecimal
@param Cart cart

@template.layouts.main(title = "Eosa — Votre panier", content = @`

<section class="px-5 md:px-8 pt-10 pb-4">
    <div class="flex items-baseline justify-between">
        <h1 class="font-serif text-3xl font-normal">Votre panier</h1>
        <span class="text-xs text-secondary">${cart.getItems().size()} article${cart.getItems().size() > 1 ? "s" : ""}</span>
    </div>
</section>

<section class="px-5 md:px-8 pb-12 grid md:grid-cols-[1.6fr_1fr] gap-6">

    <div class="flex flex-col gap-3.5">
        @if(cart.getItems().isEmpty())
            <div class="py-16 text-center text-secondary text-sm">Votre panier est vide.</div>
        @else
            @for(CartItem item : cart.getItems())
                <div class="bg-bg border border-bg-border rounded-3xl p-4 grid grid-cols-[80px_1fr_auto] md:grid-cols-[100px_1fr_auto] gap-4 md:gap-5 items-center">
                    <div class="bg-bg-soft aspect-square rounded-2xl overflow-hidden">
                        @if(item.imageId() != null)
                            <img src="/images/${item.imageId().value()}" class="w-full h-full object-cover" alt="${item.name()}">
                        @endif
                    </div>
                    <div>
                        <div class="font-serif text-lg mb-1">${item.name()}</div>
                        <div class="flex gap-3 items-center mt-3">
                            <div class="flex items-center bg-bg-soft rounded-full">
                                <button class="w-7 h-7 text-secondary text-sm"
                                        hx-patch="/cart/items/${item.productId().value()}"
                                        hx-vals='{"quantity": "${item.quantity() - 1}"}'
                                        hx-target="body" hx-swap="outerHTML">−</button>
                                <span class="px-3 text-primary text-[13px]">${item.quantity()}</span>
                                <button class="w-7 h-7 text-secondary text-sm"
                                        hx-patch="/cart/items/${item.productId().value()}"
                                        hx-vals='{"quantity": "${item.quantity() + 1}"}'
                                        hx-target="body" hx-swap="outerHTML">+</button>
                            </div>
                            <button class="text-xs text-muted hover:text-secondary transition-colors"
                                    hx-delete="/cart/items/${item.productId().value()}"
                                    hx-target="body" hx-swap="outerHTML">Retirer</button>
                        </div>
                    </div>
                    <div class="font-serif text-lg self-start pt-1">${item.price().multiply(BigDecimal.valueOf(item.quantity())).stripTrailingZeros().toPlainString()} €</div>
                </div>
            @endfor
        @endif
    </div>

    <aside>
        <div class="bg-bg-soft p-6 rounded-3xl md:sticky md:top-5">
            <div class="font-serif text-xl mb-5">Récapitulatif</div>
            <div class="flex flex-col gap-3 mb-4">
                <div class="flex justify-between text-sm text-secondary">
                    <span>Sous-total</span>
                    <span class="text-primary">${cart.getItems().stream().map(i -> i.price().multiply(BigDecimal.valueOf(i.quantity()))).reduce(BigDecimal.ZERO, BigDecimal::add).stripTrailingZeros().toPlainString()} €</span>
                </div>
                <div class="flex justify-between text-sm text-secondary">
                    <span>Livraison</span><span class="text-success font-medium">Offerte</span>
                </div>
            </div>

            <div class="flex justify-between items-baseline py-4 border-t border-bg-border mb-4">
                <span class="text-sm">Total</span>
                <span class="font-serif text-3xl">${cart.getItems().stream().map(i -> i.price().multiply(BigDecimal.valueOf(i.quantity()))).reduce(BigDecimal.ZERO, BigDecimal::add).stripTrailingZeros().toPlainString()} €</span>
            </div>
            @if(cart.getItems().isEmpty())
                <button disabled class="w-full bg-bg-soft text-muted py-4 rounded-full text-sm font-medium cursor-not-allowed opacity-60">Passer au paiement →</button>
            @else
                <form method="post" action="/checkout">
                    <button type="submit" class="w-full bg-primary text-accent py-4 rounded-full text-sm font-medium hover:bg-primary-dark transition-colors">Passer au paiement →</button>
                </form>
            @endif
            <div class="mt-4 flex flex-col gap-2 text-xs text-secondary">
                <div>★ Livraison offerte dès 60 €</div>
                <div>♡ Emballage cadeau offert</div>
            </div>
        </div>
    </aside>

</section>

`)