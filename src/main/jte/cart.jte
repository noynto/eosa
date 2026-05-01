<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Lithée — Votre panier</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Bagel+Fat+One&family=DM+Sans:wght@400;500&family=Cormorant+Garamond:ital,wght@0,400;0,500;1,400&display=swap" rel="stylesheet">
    <script src="https://cdn.tailwindcss.com"></script>
    <script>
        tailwind.config = {
            theme: {
                extend: {
                    colors: {
                        bg: '#FDFBF7',
                        'bg-soft': '#F4EFE5',
                        'bg-border': '#F0E6D6',
                        primary: '#601414',
                        'primary-dark': '#3D0A0A',
                        secondary: '#775546',
                        accent: '#FFEFAE',
                        muted: '#B8A88E',
                        success: '#1D9E75',
                    },
                    fontFamily: {
                        sans: ['DM Sans', 'system-ui', 'sans-serif'],
                        serif: ['Cormorant Garamond', 'Georgia', 'serif'],
                        display: ['Bagel Fat One', 'cursive'],
                    },
                },
            },
        };
    </script>
</head>
<body class="bg-bg font-sans text-primary antialiased">

<header class="px-5 md:px-8 py-5 flex items-center justify-between">
    <a href="/" class="font-display text-3xl text-primary">lithée</a>
    <nav class="hidden md:flex gap-7 text-sm text-secondary">
        <a href="/">Boutique</a>
        <a href="#">Composer</a>
        <a href="#">Pierres</a>
    </nav>
    <div class="flex gap-3 items-center">
        <button class="w-9 h-9 rounded-full bg-bg-soft flex items-center justify-center" aria-label="Recherche">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#601414" stroke-width="2"><circle cx="11" cy="11" r="7"/><path d="m21 21-4.3-4.3"/></svg>
        </button>
        <a href="/cart" class="bg-primary text-accent px-4 py-2.5 rounded-full text-[13px] flex items-center gap-2">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#FFEFAE" stroke-width="2"><path d="M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4Z"/><path d="M3 6h18"/><path d="M16 10a4 4 0 0 1-8 0"/></svg>
            Panier · 3
        </a>
    </div>
</header>

<section class="px-5 md:px-8 pt-10 pb-8">
    <h1 class="font-serif text-4xl md:text-5xl font-normal leading-none mb-2">Votre panier</h1>
    <div class="text-sm text-secondary">3 pièces sélectionnées</div>
</section>

<div class="px-5 md:px-8 pb-6 flex flex-col md:flex-row gap-2 md:gap-3.5">
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
            <div class="bg-bg-soft aspect-square rounded-2xl relative">
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
            <div class="bg-accent aspect-square rounded-2xl relative">
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

        <div class="bg-bg border border-bg-border rounded-3xl p-4 grid grid-cols-[80px_1fr_auto] md:grid-cols-[100px_1fr_auto] gap-4 md:gap-5 items-center">
            <div class="bg-bg-soft aspect-square rounded-2xl relative">
                <svg viewBox="0 0 100 100" class="w-full h-full">
                    <g transform="translate(50, 50)"><circle cx="-22" cy="0" r="6" fill="#854F0B"/><circle cx="-12" cy="-4" r="7" fill="#BA7517"/><circle cx="0" cy="-6" r="6" fill="#412402"/><circle cx="12" cy="-4" r="7" fill="#BA7517"/><circle cx="22" cy="0" r="6" fill="#854F0B"/><circle cx="22" cy="10" r="6" fill="#412402"/><circle cx="12" cy="16" r="7" fill="#BA7517"/><circle cx="0" cy="18" r="6" fill="#854F0B"/><circle cx="-12" cy="16" r="7" fill="#412402"/><circle cx="-22" cy="10" r="6" fill="#BA7517"/></g>
                </svg>
                <div class="absolute top-1.5 right-1.5 bg-accent text-primary px-2 py-0.5 rounded-full text-[9px]">Sur-mesure</div>
            </div>
            <div>
                <div class="font-serif text-lg mb-1">Création — Solaire</div>
                <div class="text-xs text-secondary mb-3">Œil de tigre · gravure « M ♡ J »</div>
                <div class="flex gap-3 items-center flex-wrap">
                    <div class="flex items-center bg-bg-soft rounded-full">
                        <button class="w-7 h-7 text-secondary text-sm">−</button>
                        <span class="px-3 text-primary text-[13px]">1</span>
                        <button class="w-7 h-7 text-secondary text-sm">+</button>
                    </div>
                    <span class="text-xs text-primary cursor-pointer">Modifier</span>
                    <span class="text-xs text-muted cursor-pointer">Retirer</span>
                </div>
            </div>
            <div class="font-serif text-lg self-start pt-1">62 €</div>
        </div>

    </div>

    <aside>
        <div class="bg-bg-soft p-6 rounded-3xl md:sticky md:top-5">
            <div class="font-serif text-xl mb-5">Récapitulatif</div>

            <div class="flex flex-col gap-3 mb-4">
                <div class="flex justify-between text-sm text-secondary">
                    <span>Sous-total</span><span class="text-primary">152 €</span>
                </div>
                <div class="flex justify-between text-sm text-secondary">
                    <span>Livraison</span><span class="text-success font-medium">Offerte</span>
                </div>
            </div>

            <div class="flex gap-1.5 mb-5">
                <input type="text" placeholder="Code promo" class="flex-1 bg-bg border-0 px-4 py-3 rounded-full text-[13px] text-primary placeholder:text-muted">
                <button class="bg-bg text-primary px-4 py-3 rounded-full text-[13px]">OK</button>
            </div>

            <div class="flex justify-between items-baseline py-4 border-t border-[#D9CFB8] mb-4">
                <span class="text-sm">Total</span>
                <span class="font-serif text-3xl">152 €</span>
            </div>

            <a href="/payment"><button class="w-full bg-primary text-accent py-4 rounded-full text-sm font-medium">Passer au paiement →</button></a>

            <div class="mt-4 flex flex-col gap-2 text-xs text-secondary">
                <div>★ Livraison offerte dès 80 €</div>
                <div>♡ Emballage cadeau offert</div>
            </div>
        </div>
    </aside>

</section>

<footer class="px-5 md:px-8 py-6 bg-primary-dark text-bg flex flex-col md:flex-row justify-between items-center gap-2 text-xs">
    <span class="font-display text-[22px] text-accent">lithée</span>
    <span class="opacity-70">Fait main en France</span>
</footer>

</body>
</html>