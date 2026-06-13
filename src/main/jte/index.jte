@import me.noynto.eosa.application.ReadCategoryStats
@param ReadCategoryStats.Stats necklaces
@param ReadCategoryStats.Stats bracelets

@template.layouts.main(title = "Eosa — Bijoux faits main à Nancy", content = @`

<!-- Hero -->
<section class="grid md:grid-cols-[1.2fr_1fr] border-b border-bg-border">
    <div class="bg-bg-soft relative overflow-hidden min-h-64 md:min-h-0 border-b md:border-b-0 md:border-r border-bg-border">
        <img src="/hero.png" alt="Bijoux Eosa" class="w-full h-full object-cover"/>
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

<!-- Nos univers -->
<section class="px-5 md:px-8 py-10">
    <div class="flex items-baseline gap-3 mb-6">
        <span class="font-serif text-xs text-muted">01</span>
        <h2 class="font-serif text-2xl font-normal">Nos univers</h2>
        <span class="text-xs text-secondary ml-1">explorer par famille</span>
    </div>
    <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <a href="/products/necklaces" class="border border-bg-border rounded-2xl p-5 flex items-center gap-5 hover:border-secondary transition-colors">
            <div class="w-14 h-14 flex-shrink-0">
                <svg viewBox="0 0 56 56" class="w-full h-full">
                    <path d="M8 18 Q28 50 48 18" stroke="#601414" stroke-width="1.5" fill="none" stroke-dasharray="2.5 2"/>
                    <circle cx="28" cy="44" r="6" stroke="#601414" stroke-width="1.5" fill="#F4EFE5"/>
                    <circle cx="28" cy="44" r="3" fill="#601414"/>
                </svg>
            </div>
            <div>
                <div class="font-serif text-xl leading-tight">Colliers</div>
                <div class="text-xs text-secondary mt-1">
                    ${necklaces.count()} pièce${necklaces.count() > 1 ? "s" : ""}
                    @if(necklaces.minPrice() != null)
                        · dès ${necklaces.minPrice().stripTrailingZeros().toPlainString()} €
                    @endif
                </div>
            </div>
        </a>
        <a href="/products/bracelets" class="border border-bg-border rounded-2xl p-5 flex items-center gap-5 hover:border-secondary transition-colors">
            <div class="w-14 h-14 flex-shrink-0">
                <svg viewBox="0 0 56 56" class="w-full h-full">
                    <ellipse cx="28" cy="28" rx="18" ry="10" stroke="#601414" stroke-width="1.5" fill="none" stroke-dasharray="2.5 2"/>
                    <circle cx="10" cy="28" r="3.5" stroke="#601414" stroke-width="1.5" fill="#F4EFE5"/>
                    <circle cx="28" cy="18" r="2.5" stroke="#601414" stroke-width="1.5" fill="#F4EFE5"/>
                    <circle cx="46" cy="28" r="3.5" stroke="#601414" stroke-width="1.5" fill="#F4EFE5"/>
                </svg>
            </div>
            <div>
                <div class="font-serif text-xl leading-tight">Bracelets</div>
                <div class="text-xs text-secondary mt-1">
                    ${bracelets.count()} pièce${bracelets.count() > 1 ? "s" : ""}
                    @if(bracelets.minPrice() != null)
                        · dès ${bracelets.minPrice().stripTrailingZeros().toPlainString()} €
                    @endif
                </div>
            </div>
        </a>
    </div>
</section>

<!-- Nouveautés -->
<section class="px-5 md:px-8 pb-12">
    <div class="flex items-baseline justify-between mb-6">
        <div class="flex items-baseline gap-3">
            <span class="font-serif text-xs text-muted">02</span>
            <h2 class="font-serif text-2xl font-normal">Nouveautés</h2>
            <span class="text-xs text-secondary ml-1">dernières arrivées</span>
        </div>
        <a href="/shop" class="text-xs text-secondary hover:text-primary transition-colors">Tout voir →</a>
    </div>
    <div class="grid grid-cols-2 md:grid-cols-4 gap-4 md:gap-5">

        <a href="/products/1" class="group">
            <div class="bg-bg-soft aspect-square rounded-3xl relative overflow-hidden">
                <svg viewBox="0 0 200 200" class="w-full h-full">
                    <ellipse cx="100" cy="148" rx="52" ry="9" fill="#775546" opacity="0.15"/>
                    <line x1="100" y1="28" x2="100" y2="72" stroke="#B8A88E" stroke-width="0.8" opacity="0.5"/>
                    <circle cx="100" cy="106" r="38" fill="#7F77DD"/>
                    <circle cx="100" cy="106" r="24" fill="#AFA9EC"/>
                    <circle cx="100" cy="106" r="12" fill="#534AB7"/>
                </svg>
                <button class="absolute top-3.5 right-3.5 w-9 h-9 bg-bg rounded-full flex items-center justify-center" aria-label="Favoris">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#601414" stroke-width="2"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.29 1.51 4.04 3 5.5l7 7Z"/></svg>
                </button>
            </div>
            <div class="mt-3.5 px-1">
                <div class="flex justify-between items-baseline mb-1">
                    <div class="font-serif text-[17px]">Collier Lune</div>
                    <div class="text-[15px] font-medium">42 €</div>
                </div>
                <div class="text-xs text-secondary">Améthyste</div>
            </div>
        </a>

        <a href="/products/2" class="group">
            <div class="bg-accent aspect-square rounded-3xl relative overflow-hidden">
                <svg viewBox="0 0 200 200" class="w-full h-full">
                    <ellipse cx="100" cy="148" rx="52" ry="9" fill="#775546" opacity="0.15"/>
                    <g transform="translate(100, 100)">
                        <circle cx="-42" cy="0" r="13" fill="#854F0B"/>
                        <circle cx="-23" cy="-9" r="15" fill="#BA7517"/>
                        <circle cx="0" cy="-13" r="13" fill="#412402"/>
                        <circle cx="23" cy="-9" r="15" fill="#BA7517"/>
                        <circle cx="42" cy="0" r="13" fill="#854F0B"/>
                        <circle cx="42" cy="18" r="13" fill="#412402"/>
                        <circle cx="23" cy="30" r="15" fill="#BA7517"/>
                        <circle cx="0" cy="34" r="13" fill="#854F0B"/>
                        <circle cx="-23" cy="30" r="15" fill="#412402"/>
                        <circle cx="-42" cy="18" r="13" fill="#BA7517"/>
                    </g>
                </svg>
                <div class="absolute top-3.5 left-3.5 bg-primary text-accent px-3 py-1 rounded-full text-[11px]">Nouveau</div>
                <button class="absolute top-3.5 right-3.5 w-9 h-9 bg-bg rounded-full flex items-center justify-center" aria-label="Favoris">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#601414" stroke-width="2"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.29 1.51 4.04 3 5.5l7 7Z"/></svg>
                </button>
            </div>
            <div class="mt-3.5 px-1">
                <div class="flex justify-between items-baseline mb-1">
                    <div class="font-serif text-[17px]">Collier Soleil</div>
                    <div class="text-[15px] font-medium">50 €</div>
                </div>
                <div class="text-xs text-secondary">Œil de tigre</div>
            </div>
        </a>

        <a href="/products/3" class="group">
            <div class="bg-bg-soft aspect-square rounded-3xl relative overflow-hidden">
                <svg viewBox="0 0 200 200" class="w-full h-full">
                    <ellipse cx="100" cy="148" rx="52" ry="9" fill="#775546" opacity="0.15"/>
                    <line x1="100" y1="28" x2="100" y2="72" stroke="#B8A88E" stroke-width="0.8" opacity="0.5"/>
                    <circle cx="100" cy="106" r="36" fill="#ED93B1"/>
                    <circle cx="100" cy="106" r="23" fill="#F4C0D1"/>
                    <circle cx="100" cy="106" r="11" fill="#D4537E"/>
                </svg>
                <button class="absolute top-3.5 right-3.5 w-9 h-9 bg-bg rounded-full flex items-center justify-center" aria-label="Favoris">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#601414" stroke-width="2"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.29 1.51 4.04 3 5.5l7 7Z"/></svg>
                </button>
            </div>
            <div class="mt-3.5 px-1">
                <div class="flex justify-between items-baseline mb-1">
                    <div class="font-serif text-[17px]">Collier Aurore</div>
                    <div class="text-[15px] font-medium">58 €</div>
                </div>
                <div class="text-xs text-secondary">Quartz rose</div>
            </div>
        </a>

        <a href="/products/4" class="group">
            <div class="bg-bg-soft aspect-square rounded-3xl relative overflow-hidden">
                <svg viewBox="0 0 200 200" class="w-full h-full">
                    <ellipse cx="100" cy="148" rx="52" ry="9" fill="#775546" opacity="0.15"/>
                    <g transform="translate(100, 100)">
                        <circle cx="-42" cy="0" r="13" fill="#185FA5"/>
                        <circle cx="-23" cy="-9" r="15" fill="#378ADD"/>
                        <circle cx="0" cy="-13" r="13" fill="#0C447C"/>
                        <circle cx="23" cy="-9" r="15" fill="#185FA5"/>
                        <circle cx="42" cy="0" r="13" fill="#378ADD"/>
                        <circle cx="42" cy="18" r="13" fill="#0C447C"/>
                        <circle cx="23" cy="30" r="15" fill="#185FA5"/>
                        <circle cx="0" cy="34" r="13" fill="#378ADD"/>
                        <circle cx="-23" cy="30" r="15" fill="#0C447C"/>
                        <circle cx="-42" cy="18" r="13" fill="#185FA5"/>
                    </g>
                </svg>
                <button class="absolute top-3.5 right-3.5 w-9 h-9 bg-bg rounded-full flex items-center justify-center" aria-label="Favoris">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#601414" stroke-width="2"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.29 1.51 4.04 3 5.5l7 7Z"/></svg>
                </button>
            </div>
            <div class="mt-3.5 px-1">
                <div class="flex justify-between items-baseline mb-1">
                    <div class="font-serif text-[17px]">Collier Marée</div>
                    <div class="text-[15px] font-medium">46 €</div>
                </div>
                <div class="text-xs text-secondary">Labradorite</div>
            </div>
        </a>

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
        <img src="/workshop.png" alt="Atelier Eosa" class="w-full h-full object-cover"/>
    </div>
</section>

`)