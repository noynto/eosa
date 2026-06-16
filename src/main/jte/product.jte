@import me.noynto.eosa.product.Product
@import me.noynto.eosa.shared.ProductId
@import java.util.List
@param Product product
@param List<ProductId> relatedIds

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
            <h1 class="font-serif text-4xl font-normal leading-tight mb-1.5">${product.getName()}</h1>
            <div class="text-sm text-secondary mb-3.5">${product.getTagline()}</div>
            <div class="flex items-baseline gap-3">
                <span class="font-serif text-3xl">${product.getPrice().stripTrailingZeros().toPlainString()} €</span>

            </div>
        </div>

        <div class="flex gap-2.5">
            <button class="flex-1 bg-primary text-accent py-4 rounded-full text-sm font-medium hover:bg-primary-dark transition-colors"
                    hx-post="/cart/items/${product.getId().value()}"
                    hx-swap="none">Ajouter au panier — ${product.getPrice().stripTrailingZeros().toPlainString()} €</button>
        </div>

        <div class="bg-bg-soft px-5 py-4 rounded-2xl flex flex-col gap-2.5 text-[13px] text-secondary">
            <div hx-get="/shipping/info" hx-trigger="load" hx-swap="outerHTML" class="flex items-center gap-2.5">
                <div class="w-4 h-4 rounded bg-bg-border animate-pulse flex-shrink-0"></div>
                <div class="w-56 h-3 rounded-full bg-bg-border animate-pulse"></div>
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
        <div class="py-3.5 border-b border-bg-border md:pr-6 flex justify-between"><span class="text-[13px] text-secondary">Matière</span><span class="text-[13px]">Perles et pierres non précieuses</span></div>
        <div class="py-3.5 border-b border-bg-border md:pl-6 flex justify-between"><span class="text-[13px] text-secondary">Fil</span><span class="text-[13px]">Câble tressé, gaine nylon</span></div>
        <div class="py-3.5 border-b border-bg-border md:pr-6 flex justify-between"><span class="text-[13px] text-secondary">Métal</span><span class="text-[13px]">Acier inoxydable</span></div>
        <div class="py-3.5 border-b border-bg-border md:pl-6 flex justify-between"><span class="text-[13px] text-secondary">Fermoir</span><span class="text-[13px]">Acier inoxydable</span></div>
        <div class="py-3.5 md:border-b-0 md:pr-6 flex justify-between"><span class="text-[13px] text-secondary">Entretien</span><span class="text-[13px]">Eau claire, séchage doux</span></div>
    </div>
</section>

@if(!relatedIds.isEmpty())
<section class="px-5 md:px-8 pt-8 pb-12">
    <div class="flex justify-between items-baseline mb-5">
        <h2 class="font-serif text-2xl font-normal">D'autres bijoux</h2>
        <a href="/products" class="text-[13px] text-secondary hover:text-primary transition-colors">Voir tout →</a>
    </div>
    <div class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3.5">
        @for(var relatedId : relatedIds)
            <div hx-get="/products/${relatedId.value()}/card" hx-trigger="load" hx-swap="outerHTML">
                <div class="bg-bg-soft aspect-square rounded-[20px] animate-pulse"></div>
            </div>
        @endfor
    </div>
</section>
@endif

`)