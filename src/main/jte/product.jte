@import me.noynto.eosa.product.Product
@param Product product

@template.layouts.main(title = "Eosa — " + product.getName(), content = @`

<nav class="px-5 md:px-8 py-3.5 text-xs text-muted border-b border-bg-border">
    <a href="/" class="hover:text-secondary transition-colors">Boutique</a><span class="mx-1.5">›</span><a href="/products" class="hover:text-secondary transition-colors">Produits</a><span class="mx-1.5">›</span><span class="text-primary">${product.getName()}</span>
</nav>

<section class="px-5 md:px-8 pt-6 pb-12 grid md:grid-cols-[1.2fr_1fr] gap-10">

    <div class="flex flex-col-reverse md:flex-row gap-3.5">
        <div class="flex md:flex-col gap-2.5 overflow-x-auto md:overflow-visible">
            @if(product.getImageIds().isEmpty())
                <div class="w-16 h-16 bg-bg-soft rounded-xl border-2 border-primary relative overflow-hidden flex-shrink-0">
                    <svg viewBox="0 0 64 64" class="w-full h-full">
                        <circle cx="32" cy="32" r="20" fill="#B8A88E" opacity="0.4"/>
                    </svg>
                </div>
            @else
                <div class="w-16 h-16 bg-bg-soft rounded-xl border-2 border-primary relative overflow-hidden flex-shrink-0 cursor-pointer"
                     onclick="document.getElementById('main-image').src='/images/${product.getImageIds().getFirst().value()}'">
                    <img src="/images/${product.getImageIds().getFirst().value()}" class="w-full h-full object-cover" alt="${product.getName()}">
                </div>
                @for(var imageId : product.getImageIds().subList(1, product.getImageIds().size()))
                    <div class="w-16 h-16 bg-bg-soft rounded-xl relative overflow-hidden flex-shrink-0 cursor-pointer"
                         onclick="document.getElementById('main-image').src='/images/${imageId.value()}'">
                        <img src="/images/${imageId.value()}" class="w-full h-full object-cover" alt="${product.getName()}">
                    </div>
                @endfor
            @endif
        </div>

        <div class="flex-1 bg-bg-soft aspect-square rounded-3xl relative overflow-hidden">
            @if(!product.getImageIds().isEmpty())
                <img id="main-image" src="/images/${product.getImageIds().getFirst().value()}" class="w-full h-full object-cover" alt="${product.getName()}">
            @else
                <svg viewBox="0 0 400 400" class="w-full h-full">
                    <ellipse cx="200" cy="280" rx="120" ry="16" fill="#775546" opacity="0.2"/>
                    <circle cx="200" cy="180" r="80" fill="#B8A88E" opacity="0.3"/>
                </svg>
            @endif
            <div class="absolute bottom-4 right-4 bg-bg px-3.5 py-2 rounded-full text-[11px] text-primary flex items-center gap-1.5">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#601414" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="M11 8v6M8 11h6"/></svg>
                Zoom
            </div>
        </div>
    </div>

    <div class="flex flex-col gap-6 pt-2">
        <div>
            <span class="text-xs text-success flex items-center gap-1 mb-2.5">
                <span class="w-1.5 h-1.5 rounded-full bg-success"></span>
                En stock
            </span>
            <h1 class="font-serif text-4xl font-normal leading-tight mb-1.5">${product.getName()}</h1>
            <div class="text-sm text-secondary mb-3.5">${product.getDescription()}</div>
            <div class="flex items-baseline gap-3">
                <span class="font-serif text-3xl">${product.getPrice().stripTrailingZeros().toPlainString()} €</span>
                <span class="text-xs text-secondary">ou 3× avec Klarna</span>
            </div>
        </div>

        <div>
            <div class="flex justify-between items-baseline mb-2.5">
                <span class="text-[13px] font-medium">Taille du poignet</span>
                <span class="text-xs text-secondary underline cursor-pointer">Guide des tailles</span>
            </div>
            <div class="flex gap-2">
                <span class="flex-1 text-center py-3 bg-bg-soft rounded-full text-[13px] cursor-pointer">15 cm</span>
                <span class="flex-1 text-center py-3 bg-primary text-accent rounded-full text-[13px] cursor-pointer">17 cm</span>
                <span class="flex-1 text-center py-3 bg-bg-soft rounded-full text-[13px] cursor-pointer">19 cm</span>
                <span class="flex-1 text-center py-3 bg-bg-soft rounded-full text-[13px] cursor-pointer">+1 €</span>
            </div>
        </div>

        <div>
            <div class="text-[13px] font-medium mb-2.5">Calibre des perles</div>
            <div class="flex gap-2">
                <span class="flex-1 text-center py-3 bg-bg-soft rounded-full text-[13px] cursor-pointer">6 mm</span>
                <span class="flex-1 text-center py-3 bg-primary text-accent rounded-full text-[13px] cursor-pointer">8 mm</span>
                <span class="flex-1 text-center py-3 bg-bg-soft rounded-full text-[13px] cursor-pointer">10 mm</span>
            </div>
        </div>

        <div class="flex gap-2.5">
            <button disabled class="flex-1 bg-bg-soft text-muted py-4 rounded-full text-sm font-medium cursor-not-allowed opacity-60">Ajouter au panier — ${product.getPrice().stripTrailingZeros().toPlainString()} €</button>
            <button class="bg-bg-soft rounded-full flex items-center justify-center" style="width: 52px; height: 52px;" aria-label="Favoris">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#601414" stroke-width="2"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.29 1.51 4.04 3 5.5l7 7Z"/></svg>
            </button>
        </div>

        <div class="bg-bg-soft px-5 py-4 rounded-2xl flex flex-col gap-2.5 text-[13px] text-secondary">
            <div class="flex items-center gap-2.5">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#601414" stroke-width="2"><rect x="1" y="3" width="15" height="13" rx="2"/><path d="m16 8 4-2v10l-4-2"/></svg>
                Livraison offerte dès 60 € · expédié sous 48h
            </div>
            <div class="flex items-center gap-2.5">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#601414" stroke-width="2"><path d="M20 12V8H4v10h16v-3"/><path d="M16 11h6"/><path d="m19 8-3 3 3 3"/></svg>
                Retours gratuits sous 14 jours
            </div>
            <div class="flex items-center gap-2.5">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#601414" stroke-width="2"><path d="M12 2 9 9l-7 1 5 5-1 7 6-3 6 3-1-7 5-5-7-1Z"/></svg>
                Fait main dans notre atelier à Nancy
            </div>
        </div>
    </div>

</section>

<section class="px-5 md:px-8 py-10 border-t border-bg-border">
    <h2 class="font-serif text-2xl font-normal mb-5">Détails</h2>
    <div class="grid md:grid-cols-2">
        <div class="py-3.5 border-b border-bg-border md:pr-6 flex justify-between"><span class="text-[13px] text-secondary">Matière</span><span class="text-[13px]">Améthyste naturelle</span></div>
        <div class="py-3.5 border-b border-bg-border md:pl-6 flex justify-between"><span class="text-[13px] text-secondary">Origine</span><span class="text-[13px]">Brésil</span></div>
        <div class="py-3.5 border-b border-bg-border md:pr-6 flex justify-between"><span class="text-[13px] text-secondary">Calibre</span><span class="text-[13px]">8 mm</span></div>
        <div class="py-3.5 border-b border-bg-border md:pl-6 flex justify-between"><span class="text-[13px] text-secondary">Fil</span><span class="text-[13px]">Élastique tressé</span></div>
        <div class="py-3.5 border-b border-bg-border md:border-b-0 md:pr-6 flex justify-between"><span class="text-[13px] text-secondary">Fermoir</span><span class="text-[13px]">Sans (extensible)</span></div>
        <div class="py-3.5 md:pl-6 flex justify-between"><span class="text-[13px] text-secondary">Entretien</span><span class="text-[13px]">Eau claire, séchage doux</span></div>
    </div>
</section>

<section class="px-5 md:px-8 pt-8 pb-12">
    <div class="flex justify-between items-baseline mb-5">
        <h2 class="font-serif text-2xl font-normal">Vous aimerez aussi</h2>
        <a href="/products" class="text-[13px] text-secondary hover:text-primary transition-colors">Voir tout →</a>
    </div>
    <div class="grid grid-cols-2 md:grid-cols-4 gap-3.5">
        <div hx-get="/products/${product.getId().value()}/card" hx-trigger="load" hx-swap="outerHTML">
            <div class="bg-bg-soft aspect-square rounded-[20px] animate-pulse"></div>
        </div>
        <div hx-get="/products/${product.getId().value()}/card" hx-trigger="load" hx-swap="outerHTML">
            <div class="bg-bg-soft aspect-square rounded-[20px] animate-pulse"></div>
        </div>
        <div hx-get="/products/${product.getId().value()}/card" hx-trigger="load" hx-swap="outerHTML">
            <div class="bg-bg-soft aspect-square rounded-[20px] animate-pulse"></div>
        </div>
        <div hx-get="/products/${product.getId().value()}/card" hx-trigger="load" hx-swap="outerHTML">
            <div class="bg-bg-soft aspect-square rounded-[20px] animate-pulse"></div>
        </div>
    </div>
</section>

`)