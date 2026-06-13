@import gg.jte.Content
@param String title
@param Content content
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${title}</title>
    <link rel="icon" type="image/jpeg" href="/favicon.jpg">
    <link rel="manifest" href="/manifest.json">
    <meta name="theme-color" content="#601414">
    <meta name="mobile-web-app-capable" content="yes">
    <meta name="apple-mobile-web-app-capable" content="yes">
    <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent">
    <link rel="stylesheet" href="/fonts.css">
    <script src="/tailwind.min.js"></script>
    <script src="/htmx.min.js"></script>
    <script src="/tailwind.config.js"></script>
</head>
<body class="bg-bg font-sans text-primary antialiased">

<div class="bg-accent text-primary text-center text-xs py-2 border-b border-bg-border tracking-wide">
    Site en cours de développement — certaines fonctionnalités ne sont pas encore disponibles.
</div>
<div class="bg-primary text-accent text-center text-xs py-1.5 tracking-wide">
    ✶ Livraison offerte dès 60 € ✶
</div>

<header class="border-b border-bg-border">
    <input type="checkbox" id="mobile-menu-toggle" class="peer hidden">
    <div class="grid grid-cols-[auto_1fr_auto] md:grid-cols-[1fr_auto_1fr] items-center px-5 md:px-8 py-4 gap-4">
        <nav class="hidden md:flex gap-6 text-sm text-secondary">
            <a href="/products/necklaces" class="hover:text-primary transition-colors">Colliers</a>
            <a href="/products/bracelets" class="hover:text-primary transition-colors">Bracelets</a>
        </nav>
        <label for="mobile-menu-toggle" class="md:hidden flex flex-col gap-1.5 cursor-pointer" aria-label="Menu">
            <span class="w-5 h-px bg-primary block"></span>
            <span class="w-4 h-px bg-primary block"></span>
            <span class="w-5 h-px bg-primary block"></span>
        </label>
        <div class="text-center">
            <a href="/" class="font-display text-4xl text-primary leading-none">eosa.</a>
        </div>
        <div class="flex items-center gap-3 justify-end">
            <span class="hidden md:block text-xs text-secondary">FR / €</span>
            <button class="hidden md:flex w-8 h-8 rounded-full bg-bg-soft items-center justify-center" aria-label="Recherche">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#601414" stroke-width="2"><circle cx="11" cy="11" r="7"/><path d="m21 21-4.3-4.3"/></svg>
            </button>
            <button class="hidden md:flex w-8 h-8 rounded-full bg-bg-soft items-center justify-center" aria-label="Favoris">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#601414" stroke-width="2"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.29 1.51 4.04 3 5.5l7 7Z"/></svg>
            </button>
            <a href="/cart" class="bg-bg-soft text-primary px-4 py-2 rounded-full text-[13px] flex items-center gap-2 hover:bg-bg-border transition-colors">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4Z"/><path d="M3 6h18"/><path d="M16 10a4 4 0 0 1-8 0"/></svg>
                Panier
            </a>
        </div>
    </div>
    <nav class="hidden peer-checked:flex md:hidden flex-col border-t border-bg-border text-sm text-secondary">
        <a href="/products/necklaces" class="px-5 py-3.5 hover:text-primary hover:bg-bg-soft transition-colors">Colliers</a>
        <a href="/products/bracelets" class="px-5 py-3.5 hover:text-primary hover:bg-bg-soft transition-colors">Bracelets</a>
    </nav>
</header>

${content}

<footer class="border-t border-bg-border">
    <div class="grid grid-cols-2 md:grid-cols-[1.4fr_1fr_1fr_1fr] gap-8 px-5 md:px-8 py-10">
        <div class="col-span-2 md:col-span-1">
            <a href="/" class="font-display text-3xl text-primary block mb-3">eosa</a>
            <p class="text-xs text-secondary leading-relaxed mb-5 max-w-[200px]">Bijoux faits main à Nancy. Des pièces en matières nobles.</p>
            <div class="flex gap-2.5">
                <a href="#" class="w-8 h-8 rounded-full border border-bg-border flex items-center justify-center text-secondary hover:border-secondary transition-colors" aria-label="Instagram">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="2" width="20" height="20" rx="5"/><circle cx="12" cy="12" r="5"/><circle cx="17.5" cy="6.5" r="1" fill="currentColor" stroke="none"/></svg>
                </a>
                <a href="#" class="w-8 h-8 rounded-full border border-bg-border flex items-center justify-center text-secondary hover:border-secondary transition-colors" aria-label="Pinterest">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2a10 10 0 0 0-3.6 19.3c-.1-.9-.2-2.2.1-3.2l1-4.3s-.3-.5-.3-1.3c0-1.2.7-2.1 1.7-2.1.8 0 1.2.6 1.2 1.4 0 .8-.5 2.1-.8 3.2-.2.9.5 1.7 1.4 1.7 1.7 0 2.9-2.2 2.9-4.8 0-2-1.4-3.4-3.4-3.4-2.3 0-3.7 1.7-3.7 3.5 0 .7.3 1.4.6 1.8-.1.3-.2.7-.2.9-.1.4-.3.5-.3.5-.2-.1-1.5-.7-1.5-2.7 0-2.2 1.6-4.2 4.6-4.2 2.4 0 4.3 1.7 4.3 4 0 2.4-1.5 4.3-3.6 4.3-.7 0-1.4-.4-1.6-.8l-.4 1.7c-.2.6-.5 1.3-.8 1.8.6.2 1.2.3 1.8.3a10 10 0 1 0 0-20Z"/></svg>
                </a>
                <a href="#" class="w-8 h-8 rounded-full border border-bg-border flex items-center justify-center text-secondary hover:border-secondary transition-colors" aria-label="TikTok">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 12a4 4 0 1 0 4 4V4a5 5 0 0 0 5 5"/></svg>
                </a>
            </div>
        </div>
        <div>
            <h3 class="text-sm font-medium mb-4">Boutique</h3>
            <ul class="flex flex-col gap-2.5 text-xs text-secondary">
                <li><a href="/products/necklaces" class="hover:text-primary transition-colors">Colliers</a></li>
                <li><a href="/products/bracelets" class="hover:text-primary transition-colors">Bracelets</a></li>
            </ul>
        </div>
        <div>
            <h3 class="text-sm font-medium mb-4">Aide</h3>
            <ul class="flex flex-col gap-2.5 text-xs text-secondary">
                <li><a href="#" class="hover:text-primary transition-colors">Livraison</a></li>
                <li><a href="#" class="hover:text-primary transition-colors">Retours</a></li>
                <li><a href="#" class="hover:text-primary transition-colors">FAQ</a></li>
                <li><a href="#" class="hover:text-primary transition-colors">Contact</a></li>
            </ul>
        </div>
        <div>
            <h3 class="text-sm font-medium mb-4">Newsletter</h3>
            <p class="text-xs text-secondary mb-3 leading-relaxed">Les nouvelles pièces en avant-première.</p>
            <div class="flex gap-2">
                <input type="email" placeholder="votre e-mail" class="flex-1 min-w-0 bg-bg border border-bg-border rounded-full px-3 py-2 text-xs text-primary placeholder-muted focus:outline-none focus:border-secondary">
                <button class="bg-primary text-accent px-4 py-2 rounded-full text-xs flex-shrink-0">→</button>
            </div>
        </div>
    </div>
    <div class="px-5 md:px-8 py-4 border-t border-bg-border flex flex-col md:flex-row justify-between items-center gap-2 text-xs text-muted">
        <span>© 2025 Eosa · Fait main à Nancy · France</span>
        <div class="flex gap-4">
            <a href="/legal" class="hover:text-secondary transition-colors">Mentions légales</a>
            <a href="/terms" class="hover:text-secondary transition-colors">CGV</a>
            <a href="/privacy" class="hover:text-secondary transition-colors">Confidentialité</a>
        </div>
    </div>
</footer>

<script>if ('serviceWorker' in navigator) navigator.serviceWorker.register('/sw.js');</script>
</body>
</html>