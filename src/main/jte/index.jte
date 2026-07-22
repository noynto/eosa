@import me.noynto.eosa.shared.ProductId
@import java.util.List
@param List<ProductId> latestProductIds

@template.layouts.main(title = "Eosa — Bijoux faits main à Nancy", content = @`

<!-- Hero -->
<section class="grid md:grid-cols-[1.2fr_1fr] border-b border-bg-border">
    <div class="bg-bg-soft relative overflow-hidden min-h-64 md:min-h-0 border-b md:border-b-0 md:border-r border-bg-border">
        <img src="/hero.webp" alt="Bijoux Eosa" class="w-full h-full object-cover"/>
    </div>
    <div class="px-8 md:px-12 py-12 md:py-16 flex flex-col justify-center">
        <p class="text-xs text-secondary tracking-widest mb-5">NOUVELLE COLLECTION</p>
        <h1 class="font-serif text-5xl md:text-6xl font-normal leading-[1.05] mb-6">
            Bijoux qui<br/>rayonnent.
        </h1>
        <p class="text-sm text-secondary leading-relaxed mb-8 max-w-xs">
            Colliers, bracelets et pierres, faits main à Nancy. Des pièces en matières nobles, conçues pour durer.
        </p>
        <div class="flex gap-3 flex-wrap">
            <a href="/products" class="bg-secondary text-bg px-6 py-3 rounded-full text-sm font-medium hover:bg-primary transition-colors">Voir la collection</a>
            <a href="/about" class="border border-bg-border px-6 py-3 rounded-full text-sm text-secondary hover:border-secondary transition-colors">Notre histoire</a>
        </div>
    </div>
</section>

<!-- Nouveautés -->
<section class="px-5 md:px-8 py-10">
    <div class="flex items-baseline justify-between mb-6">
        <div class="flex items-baseline gap-3">
            <span class="font-serif text-xs text-muted">01</span>
            <h2 class="font-serif text-2xl font-normal">Nouveautés</h2>
            <span class="text-xs text-secondary ml-1">dernières arrivées</span>
        </div>
        <a href="/products" class="text-xs text-secondary hover:text-primary transition-colors">Tout voir →</a>
    </div>
    <div class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4 lg:gap-5">
        @for(var productId : latestProductIds)
            <div hx-get="/products/${productId.value()}/card"
                 hx-trigger="load"
                 hx-swap="outerHTML">
                <div class="bg-bg-soft aspect-square rounded-3xl animate-pulse"></div>
                <div class="mt-3.5 px-1 flex flex-col gap-2">
                    <div class="h-4 bg-bg-soft rounded-full animate-pulse w-3/4"></div>
                    <div class="h-3 bg-bg-soft rounded-full animate-pulse w-1/3"></div>
                </div>
            </div>
        @endfor
    </div>
</section>

<!-- Atelier editorial -->
<section class="grid md:grid-cols-2 border-t border-bg-border">
    <div class="px-8 md:px-12 py-12 md:py-16 flex flex-col justify-center order-2 md:order-1">
        <p class="text-xs text-secondary tracking-widest mb-3">L'ATELIER</p>
        <h2 class="font-serif text-4xl font-normal leading-tight mb-5">Faits main,<br/>montés à Nancy.</h2>
        <p class="text-sm text-secondary leading-relaxed mb-7 max-w-sm">
            Chaque bijou est pensé, dessiné et assemblé dans notre atelier. Des matières soigneusement sélectionnées, des pièces conçues pour traverser le temps.
        </p>
        <a href="/about" class="border border-bg-border px-6 py-3 rounded-full text-sm text-secondary self-start hover:border-secondary transition-colors">En savoir plus</a>
    </div>
    <div class="bg-bg-soft relative overflow-hidden min-h-64 order-1 md:order-2 border-b md:border-b-0 md:border-l border-bg-border">
        <img src="/workshop.webp" alt="Atelier Eosa" class="w-full h-full object-cover"/>
    </div>
</section>

`)