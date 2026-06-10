@import gg.jte.Content
@param String title
@param String backHref
@param String backLabel
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

<header class="border-b border-bg-border">
    <div class="grid grid-cols-[auto_1fr_auto] items-center px-5 md:px-8 py-4 gap-4">
        <a href="${backHref}" class="text-[13px] text-secondary hover:text-primary transition-colors flex items-center gap-1.5">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m15 18-6-6 6-6"/></svg>
            ${backLabel}
        </a>
        <div class="text-center">
            <a href="/" class="font-display text-4xl text-primary leading-none">eosa.</a>
        </div>
        <span class="text-xs text-secondary hidden md:flex items-center gap-1.5">
            <svg width="13" height="13" viewBox="0 0 16 16"><path d="M8 1 L13 4 V8 C13 11 8 14 8 14 C8 14 3 11 3 8 V4 Z" fill="none" stroke="#775546" stroke-width="1.2"/></svg>
            Paiement sécurisé
        </span>
        <span class="md:hidden"></span>
    </div>
</header>

${content}

<footer class="border-t border-bg-border px-5 md:px-8 py-5 flex flex-col md:flex-row justify-between items-center gap-2 text-xs text-muted">
    <a href="/" class="font-display text-2xl text-primary">eosa</a>
    <div class="flex gap-4">
        <a href="#" class="hover:text-secondary transition-colors">Mentions légales</a>
        <a href="#" class="hover:text-secondary transition-colors">CGV</a>
        <a href="#" class="hover:text-secondary transition-colors">Contact</a>
    </div>
    <span>© 2025 Eosa · Fait main à Lyon</span>
</footer>

<script>if ('serviceWorker' in navigator) navigator.serviceWorker.register('/sw.js');</script>
</body>
</html>