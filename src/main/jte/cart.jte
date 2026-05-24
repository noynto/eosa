@template.layouts.checkout(title = "Eosa — Votre panier", backHref = "/", backLabel = "Boutique", content = @`

<div class="px-5 md:px-8 pt-8 pb-6 flex flex-col md:flex-row gap-2 md:gap-3.5">
    <div class="flex-1 bg-primary px-5 py-3.5 rounded-full flex items-center gap-3">
        <span class="w-6 h-6 rounded-full bg-accent text-primary flex items-center justify-center text-xs">1</span>
        <span class="text-[13px] text-accent font-medium">Panier</span>
    </div>
    <div class="flex-1 px-5 py-3.5 rounded-full flex items-center gap-3">
        <span class="w-6 h-6 rounded-full bg-bg-soft text-muted flex items-center justify-center text-xs">2</span>
        <span class="text-[13px] text-muted">Livraison</span>
    </div>
    <div class="flex-1 px-5 py-3.5 rounded-full flex items-center gap-3">
        <span class="w-6 h-6 rounded-full bg-bg-soft text-muted flex items-center justify-center text-xs">3</span>
        <span class="text-[13px] text-muted">Paiement</span>
    </div>
</div>

<section class="px-5 md:px-8 pb-12 grid md:grid-cols-[1.6fr_1fr] gap-6">

    <div class="flex flex-col gap-3.5">

        <div class="bg-bg border border-bg-border rounded-3xl p-4 grid grid-cols-[80px_1fr_auto] md:grid-cols-[100px_1fr_auto] gap-4 md:gap-5 items-center">
            <div class="bg-bg-soft aspect-square rounded-2xl">
                <svg viewBox="0 0 100 100" class="w-full h-full">
                    <g transform="translate(50, 50)"><circle cx="-22" cy="0" r="6" fill="#7F77DD"/><circle cx="-12" cy="-4" r="7" fill="#AFA9EC"/><circle cx="0" cy="-6" r="6" fill="#534AB7"/><circle cx="12" cy="-4" r="7" fill="#7F77DD"/><circle cx="22" cy="0" r="6" fill="#AFA9EC"/><circle cx="22" cy="10" r="6" fill="#534AB7"/><circle cx="12" cy="16" r="7" fill="#7F77DD"/><circle cx="0" cy="18" r="6" fill="#AFA9EC"/><circle cx="-12" cy="16" r="7" fill="#534AB7"/><circle cx="-22" cy="10" r="6" fill="#7F77DD"/></g>
                </svg>
            </div>
            <div>
                <div class="font-serif text-lg mb-1">Sérénité</div>
                <div class="text-xs text-secondary mb-3">Améthyste · 17 cm</div>
                <div class="flex gap-3 items-center">
                    <div class="flex items-center bg-bg-soft rounded-full">
                        <button class="w-7 h-7 text-secondary text-sm">−</button>
                        <span class="px-3 text-primary text-[13px]">1</span>
                        <button class="w-7 h-7 text-secondary text-sm">+</button>
                    </div>
                    <span class="text-xs text-muted cursor-pointer">Retirer</span>
                </div>
            </div>
            <div class="font-serif text-lg self-start pt-1">38 €</div>
        </div>

        <div class="bg-bg border border-bg-border rounded-3xl p-4 grid grid-cols-[80px_1fr_auto] md:grid-cols-[100px_1fr_auto] gap-4 md:gap-5 items-center">
            <div class="bg-accent aspect-square rounded-2xl">
                <svg viewBox="0 0 100 100" class="w-full h-full">
                    <line x1="50" y1="14" x2="50" y2="48" stroke="#775546" stroke-width="0.6" opacity="0.5"/>
                    <circle cx="50" cy="60" r="14" fill="#ED93B1"/>
                    <circle cx="50" cy="60" r="9" fill="#F4C0D1"/>
                    <circle cx="50" cy="60" r="5" fill="#D4537E"/>
                </svg>
            </div>
            <div>
                <div class="font-serif text-lg mb-1">Tendresse</div>
                <div class="text-xs text-secondary mb-3">Quartz rose · 45 cm</div>
                <div class="flex gap-3 items-center">
                    <div class="flex items-center bg-bg-soft rounded-full">
                        <button class="w-7 h-7 text-secondary text-sm">−</button>
                        <span class="px-3 text-primary text-[13px]">1</span>
                        <button class="w-7 h-7 text-secondary text-sm">+</button>
                    </div>
                    <span class="text-xs text-muted cursor-pointer">Retirer</span>
                </div>
            </div>
            <div class="font-serif text-lg self-start pt-1">52 €</div>
        </div>

    </div>

    <aside>
        <div class="bg-bg-soft p-6 rounded-3xl md:sticky md:top-5">
            <div class="font-serif text-xl mb-5">Récapitulatif</div>
            <div class="flex flex-col gap-3 mb-4">
                <div class="flex justify-between text-sm text-secondary">
                    <span>Sous-total</span><span class="text-primary">90 €</span>
                </div>
                <div class="flex justify-between text-sm text-secondary">
                    <span>Livraison</span><span class="text-success font-medium">Offerte</span>
                </div>
            </div>
            <div class="flex gap-1.5 mb-5">
                <input type="text" placeholder="Code promo" class="flex-1 bg-bg border-0 px-4 py-3 rounded-full text-[13px] text-primary placeholder:text-muted">
                <button class="bg-bg text-primary px-4 py-3 rounded-full text-[13px]">OK</button>
            </div>
            <div class="flex justify-between items-baseline py-4 border-t border-bg-border mb-4">
                <span class="text-sm">Total</span>
                <span class="font-serif text-3xl">90 €</span>
            </div>
            <button disabled class="w-full bg-bg-soft text-muted py-4 rounded-full text-sm font-medium cursor-not-allowed opacity-60">Passer au paiement →</button>
            <div class="mt-4 flex flex-col gap-2 text-xs text-secondary">
                <div>★ Livraison offerte dès 60 €</div>
                <div>♡ Emballage cadeau offert</div>
            </div>
        </div>
    </aside>

</section>

`)