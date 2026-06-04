@template.layouts.checkout(title = "Eosa — Adresse de livraison", backHref = "/cart", backLabel = "Panier", content = @`

<div class="px-5 md:px-8 py-6 flex items-center gap-0">
    <div class="flex flex-col items-center gap-1.5">
        <div class="w-7 h-7 rounded-full bg-bg-soft text-muted text-xs font-medium flex items-center justify-center">1</div>
        <span class="text-[10px] text-muted hidden sm:block whitespace-nowrap">Panier</span>
    </div>
    <div class="flex-1 h-px bg-bg-border mb-5 sm:mb-0 mx-2"></div>
    <div class="flex flex-col items-center gap-1.5">
        <div class="w-7 h-7 rounded-full bg-primary text-accent text-xs font-medium flex items-center justify-center">2</div>
        <span class="text-[10px] text-primary font-medium hidden sm:block whitespace-nowrap">Adresse</span>
    </div>
    <div class="flex-1 h-px bg-bg-border mb-5 sm:mb-0 mx-2"></div>
    <div class="flex flex-col items-center gap-1.5">
        <div class="w-7 h-7 rounded-full bg-bg-soft text-muted text-xs font-medium flex items-center justify-center">3</div>
        <span class="text-[10px] text-muted hidden sm:block whitespace-nowrap">Livraison</span>
    </div>
    <div class="flex-1 h-px bg-bg-border mb-5 sm:mb-0 mx-2"></div>
    <div class="flex flex-col items-center gap-1.5">
        <div class="w-7 h-7 rounded-full bg-bg-soft text-muted text-xs font-medium flex items-center justify-center">4</div>
        <span class="text-[10px] text-muted hidden sm:block whitespace-nowrap">Paiement</span>
    </div>
</div>

<section class="px-5 md:px-8 pb-12 grid md:grid-cols-[1.6fr_1fr] gap-6">

    <div>
        <h2 class="font-serif text-2xl font-normal mb-1">Adresse de livraison</h2>
        <p class="text-sm text-secondary mb-5">Recherchez votre adresse.</p>

        <div class="relative mb-4">
            <svg class="absolute left-4 top-1/2 -translate-y-1/2 text-muted pointer-events-none" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/>
            </svg>
            <input
                type="text"
                name="q"
                placeholder="Ex : 12 rue Stanislas, Nancy"
                autocomplete="off"
                class="w-full bg-bg-soft border border-bg-border rounded-2xl pl-10 pr-5 py-3.5 text-sm placeholder:text-muted focus:outline-none focus:border-primary transition-colors"
                hx-get="/checkout/physical-address/suggest"
                hx-trigger="keyup changed delay:350ms"
                hx-target="#suggestions"
                hx-include="[name='q']"
            />
        </div>

        <div id="suggestions" class="flex flex-col gap-2.5"></div>
    </div>

    <aside>
        <div class="bg-bg-soft p-6 rounded-3xl md:sticky md:top-5">
            <div class="font-serif text-xl mb-5">Récapitulatif</div>
            <div class="flex flex-col gap-3 mb-5">
                <div class="flex items-center gap-3">
                    <div class="w-10 h-10 bg-bg-border rounded-xl flex-shrink-0">
                        <svg viewBox="0 0 40 40" class="w-full h-full">
                            <circle cx="20" cy="20" r="8" fill="#B8A88E" opacity="0.4"/>
                        </svg>
                    </div>
                    <div class="flex-1 min-w-0">
                        <p class="text-[13px] font-medium truncate">Sérénité</p>
                        <p class="text-xs text-secondary">Améthyste · 17 cm</p>
                    </div>
                    <span class="font-serif text-base flex-shrink-0">38 €</span>
                </div>
                <div class="flex items-center gap-3">
                    <div class="w-10 h-10 bg-accent rounded-xl flex-shrink-0">
                        <svg viewBox="0 0 40 40" class="w-full h-full">
                            <circle cx="20" cy="24" r="6" fill="#ED93B1"/>
                        </svg>
                    </div>
                    <div class="flex-1 min-w-0">
                        <p class="text-[13px] font-medium truncate">Tendresse</p>
                        <p class="text-xs text-secondary">Quartz rose · 45 cm</p>
                    </div>
                    <span class="font-serif text-base flex-shrink-0">52 €</span>
                </div>
            </div>
            <div class="flex flex-col gap-2.5 py-4 border-t border-bg-border mb-4">
                <div class="flex justify-between text-sm text-secondary">
                    <span>Sous-total</span><span class="text-primary">90 €</span>
                </div>
                <div class="flex justify-between text-sm text-secondary">
                    <span>Livraison</span><span class="text-success font-medium">Offerte</span>
                </div>
            </div>
            <div class="flex justify-between items-baseline pb-4 border-b border-bg-border mb-4">
                <span class="text-sm">Total</span>
                <span class="font-serif text-3xl">90 €</span>
            </div>
            <div class="flex flex-col gap-1.5 text-xs text-secondary">
                <div>★ Livraison offerte dès 60 €</div>
                <div>♡ Emballage cadeau offert</div>
            </div>
        </div>
    </aside>

</section>

<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
<script src="https://unpkg.com/htmx.org@2.0.3"></script>
<script>
    function initMaps() {
        document.querySelectorAll('[data-map]:not([data-map-ready])').forEach(function(el) {
            var lat = parseFloat(el.dataset.lat);
            var lon = parseFloat(el.dataset.lon);
            if (isNaN(lat) || isNaN(lon)) return;
            var map = L.map(el, {
                zoomControl: false, attributionControl: false,
                dragging: false, touchZoom: false,
                scrollWheelZoom: false, doubleClickZoom: false,
                boxZoom: false, keyboard: false
            }).setView([lat, lon], 17);
            L.tileLayer('https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png', { maxZoom: 19, subdomains: 'abcd' }).addTo(map);
            L.marker([lat, lon], {
                icon: L.divIcon({
                    className: '',
                    html: '<div style="width:12px;height:12px;background:#601414;border:2px solid white;border-radius:50%;box-shadow:0 1px 4px rgba(0,0,0,0.35)"></div>',
                    iconSize: [12, 12], iconAnchor: [6, 6]
                })
            }).addTo(map);
            el.setAttribute('data-map-ready', '');
        });
    }
    document.body.addEventListener('htmx:afterSettle', initMaps);
</script>

`)