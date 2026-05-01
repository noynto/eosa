<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Lithée — Sérénité, bracelet en améthyste</title>
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
        <a href="/" class="text-primary font-medium">Boutique</a>
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

<nav class="px-5 md:px-8 py-3.5 text-xs text-muted">
    <a href="/">Boutique</a><span class="mx-1.5">›</span><a href="/">Bracelets</a><span class="mx-1.5">›</span><span class="text-primary">Sérénité</span>
</nav>

<section class="px-5 md:px-8 pt-4 pb-12 grid md:grid-cols-[1.2fr_1fr] gap-10">

    <div class="flex flex-col-reverse md:flex-row gap-3.5">

        <div class="flex md:flex-col gap-2.5 overflow-x-auto md:overflow-visible">
            <div class="w-16 h-16 bg-bg-soft rounded-xl border-2 border-primary relative overflow-hidden flex-shrink-0 cursor-pointer">
                <svg viewBox="0 0 64 64" class="w-full h-full">
                    <g transform="translate(32, 32)"><circle cx="-14" cy="0" r="4" fill="#7F77DD"/><circle cx="-7" cy="-3" r="5" fill="#AFA9EC"/><circle cx="0" cy="-4" r="4" fill="#534AB7"/><circle cx="7" cy="-3" r="5" fill="#7F77DD"/><circle cx="14" cy="0" r="4" fill="#AFA9EC"/><circle cx="14" cy="6" r="4" fill="#534AB7"/><circle cx="7" cy="10" r="5" fill="#7F77DD"/><circle cx="0" cy="11" r="4" fill="#AFA9EC"/><circle cx="-7" cy="10" r="5" fill="#534AB7"/><circle cx="-14" cy="6" r="4" fill="#7F77DD"/></g>
                </svg>
            </div>
            <div class="w-16 h-16 bg-bg-soft rounded-xl relative overflow-hidden flex-shrink-0 cursor-pointer">
                <svg viewBox="0 0 64 64" class="w-full h-full">
                    <circle cx="32" cy="32" r="20" fill="none" stroke="#7F77DD" stroke-width="6"/>
                </svg>
            </div>
            <div class="w-16 h-16 bg-accent rounded-xl relative overflow-hidden flex-shrink-0 cursor-pointer">
                <svg viewBox="0 0 64 64" class="w-full h-full">
                    <ellipse cx="32" cy="38" rx="14" ry="10" fill="#E5C896"/>
                    <circle cx="28" cy="34" r="3" fill="#7F77DD"/>
                    <circle cx="34" cy="36" r="3" fill="#AFA9EC"/>
                </svg>
            </div>
            <div class="w-16 h-16 bg-bg-soft rounded-xl relative overflow-hidden flex-shrink-0 cursor-pointer">
                <svg viewBox="0 0 64 64" class="w-full h-full">
                    <circle cx="32" cy="32" r="14" fill="#534AB7"/>
                    <circle cx="32" cy="32" r="9" fill="#7F77DD"/>
                    <circle cx="32" cy="32" r="4" fill="#AFA9EC"/>
                </svg>
            </div>
        </div>

        <div class="flex-1 bg-bg-soft aspect-square rounded-3xl relative overflow-hidden">
            <svg viewBox="0 0 400 400" class="w-full h-full">
                <ellipse cx="200" cy="280" rx="120" ry="16" fill="#775546" opacity="0.2"/>
                <g transform="translate(200, 200)">
                    <path d="M -100 0 Q -50 -20 0 -18 Q 50 -20 100 0 Q 95 25 50 35 Q 0 40 -50 35 Q -95 25 -100 0" fill="none" stroke="#FDFBF7" stroke-width="1" opacity="0.5"/>
                    <circle cx="-90" cy="0" r="14" fill="#7F77DD"/>
                    <circle cx="-65" cy="-10" r="16" fill="#AFA9EC"/>
                    <circle cx="-35" cy="-17" r="14" fill="#534AB7"/>
                    <circle cx="-5" cy="-19" r="16" fill="#7F77DD"/>
                    <circle cx="25" cy="-17" r="14" fill="#AFA9EC"/>
                    <circle cx="55" cy="-12" r="16" fill="#534AB7"/>
                    <circle cx="80" cy="-3" r="14" fill="#7F77DD"/>
                    <circle cx="92" cy="15" r="15" fill="#AFA9EC"/>
                    <circle cx="75" cy="32" r="14" fill="#534AB7"/>
                    <circle cx="45" cy="40" r="16" fill="#7F77DD"/>
                    <circle cx="10" cy="44" r="14" fill="#AFA9EC"/>
                    <circle cx="-25" cy="42" r="16" fill="#534AB7"/>
                    <circle cx="-58" cy="35" r="14" fill="#7F77DD"/>
                    <circle cx="-85" cy="22" r="15" fill="#AFA9EC"/>
                    <circle cx="-95" cy="8" r="14" fill="#534AB7"/>
                </g>
            </svg>
            <div class="absolute bottom-4 right-4 bg-bg px-3.5 py-2 rounded-full text-[11px] text-primary flex items-center gap-1.5">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#601414" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="M11 8v6M8 11h6"/></svg>
                Zoom
            </div>
        </div>
    </div>

    <div class="flex flex-col gap-6 pt-2">

        <div>
            <div class="flex items-center gap-2 mb-2.5">
                <span class="bg-accent text-primary px-3 py-1 rounded-full text-[11px]">Apaisement</span>
                <span class="text-xs text-success flex items-center gap-1">
          <span class="w-1.5 h-1.5 rounded-full bg-success"></span>
          En stock
        </span>
            </div>
            <h1 class="font-serif text-4xl font-normal leading-tight mb-1.5">Sérénité</h1>
            <div class="text-sm text-secondary mb-3.5">Bracelet en améthyste naturelle</div>
            <div class="flex items-baseline gap-3">
                <span class="font-serif text-3xl">38 €</span>
                <span class="text-xs text-secondary">ou 3× 12,67 € avec Klarna</span>
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
            <div class="text-[13px] font-medium mb-2.5">Calibre des pierres</div>
            <div class="flex gap-2">
                <span class="flex-1 text-center py-3 bg-bg-soft rounded-full text-[13px] cursor-pointer">6 mm</span>
                <span class="flex-1 text-center py-3 bg-primary text-accent rounded-full text-[13px] cursor-pointer">8 mm</span>
                <span class="flex-1 text-center py-3 bg-bg-soft rounded-full text-[13px] cursor-pointer">10 mm</span>
            </div>
        </div>

        <div class="flex gap-2.5">
            <button class="flex-1 bg-primary text-accent py-4 rounded-full text-sm font-medium">Ajouter au panier — 38 €</button>
            <button class="w-13 h-13 bg-bg-soft rounded-full flex items-center justify-center" style="width: 52px; height: 52px;" aria-label="Favoris">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#601414" stroke-width="2"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.29 1.51 4.04 3 5.5l7 7Z"/></svg>
            </button>
        </div>

        <div class="bg-bg-soft px-5 py-4 rounded-2xl flex flex-col gap-2.5 text-[13px] text-secondary">
            <div class="flex items-center gap-2.5">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#601414" stroke-width="2"><rect x="1" y="3" width="15" height="13" rx="2"/><path d="m16 8 4-2v10l-4-2"/></svg>
                Livraison offerte dès 80 € · expédié sous 48h
            </div>
            <div class="flex items-center gap-2.5">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#601414" stroke-width="2"><path d="M20 12V8H4v10h16v-3"/><path d="M16 11h6"/><path d="m19 8-3 3 3 3"/></svg>
                Retours gratuits sous 14 jours
            </div>
            <div class="flex items-center gap-2.5">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#601414" stroke-width="2"><path d="M12 2 9 9l-7 1 5 5-1 7 6-3 6 3-1-7 5-5-7-1Z"/></svg>
                Fait main dans notre atelier en France
            </div>
        </div>

    </div>

</section>

<section class="px-5 md:px-8 py-8 md:py-12 bg-accent">
    <div class="grid md:grid-cols-[1fr_1.5fr] gap-10 items-center">
        <div>
            <div class="text-xs text-secondary tracking-wider mb-2">L'AMÉTHYSTE</div>
            <h2 class="font-serif text-3xl font-normal leading-tight mb-3.5">La pierre du calme intérieur</h2>
            <p class="text-sm text-secondary leading-relaxed m-0">L'améthyste apaise le mental et favorise la concentration. Portée au quotidien, elle accompagne les périodes de transition et invite à la sérénité.</p>
        </div>

        <div class="grid grid-cols-2 gap-3">
            <div class="bg-bg p-4 rounded-2xl">
                <div class="w-7 h-7 bg-bg-soft rounded-full flex items-center justify-center mb-2.5">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#601414" stroke-width="2"><path d="M6 3v18M18 3v18M6 12h12"/></svg>
                </div>
                <div class="text-sm font-medium mb-0.5">Apaisement</div>
                <div class="text-xs text-secondary">Réduit l'anxiété</div>
            </div>
            <div class="bg-bg p-4 rounded-2xl">
                <div class="w-7 h-7 bg-bg-soft rounded-full flex items-center justify-center mb-2.5">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#601414" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>
                </div>
                <div class="text-sm font-medium mb-0.5">Sommeil</div>
                <div class="text-xs text-secondary">Favorise le repos</div>
            </div>
            <div class="bg-bg p-4 rounded-2xl">
                <div class="w-7 h-7 bg-bg-soft rounded-full flex items-center justify-center mb-2.5">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#601414" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M12 1v6M12 17v6M4.2 4.2l4.3 4.3M15.5 15.5l4.3 4.3M1 12h6M17 12h6"/></svg>
                </div>
                <div class="text-sm font-medium mb-0.5">Clarté</div>
                <div class="text-xs text-secondary">Aiguise l'esprit</div>
            </div>
            <div class="bg-bg p-4 rounded-2xl">
                <div class="w-7 h-7 bg-bg-soft rounded-full flex items-center justify-center mb-2.5">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#601414" stroke-width="2"><path d="M12 2 L13 9 L20 9 L14 13 L17 20 L12 16 L7 20 L10 13 L4 9 L11 9 Z"/></svg>
                </div>
                <div class="text-sm font-medium mb-0.5">Intuition</div>
                <div class="text-xs text-secondary">Ouvre la perception</div>
            </div>
        </div>
    </div>
</section>

<section class="px-5 md:px-8 py-10">
    <h2 class="font-serif text-2xl font-normal mb-5">Détails</h2>
    <div class="grid md:grid-cols-2">
        <div class="py-3.5 border-b border-bg-border md:pr-6 flex justify-between"><span class="text-[13px] text-secondary">Pierre</span><span class="text-[13px]">Améthyste naturelle</span></div>
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
        <a href="" class="text-[13px] text-secondary">Voir tout →</a>
    </div>

    <div class="grid grid-cols-2 md:grid-cols-4 gap-3.5">

        <a href="/products/1" class="group">
            <div class="bg-bg-soft aspect-square rounded-[20px] relative overflow-hidden">
                <svg viewBox="0 0 100 100" class="w-full h-full">
                    <line x1="50" y1="14" x2="50" y2="48" stroke="#775546" stroke-width="0.5" opacity="0.5"/>
                    <circle cx="50" cy="60" r="14" fill="#ED93B1"/><circle cx="50" cy="60" r="9" fill="#F4C0D1"/><circle cx="50" cy="60" r="5" fill="#D4537E"/>
                </svg>
            </div>
            <div class="mt-2.5 px-1">
                <div class="flex justify-between items-baseline mb-0.5">
                    <div class="font-serif text-sm">Tendresse</div>
                    <div class="text-[13px]">52 €</div>
                </div>
                <div class="text-[11px] text-secondary">Quartz rose</div>
            </div>
        </a>

        <a href="/products/1" class="group">
            <div class="bg-bg-soft aspect-square rounded-[20px] relative overflow-hidden">
                <svg viewBox="0 0 100 100" class="w-full h-full">
                    <g transform="translate(50, 50)"><circle cx="-22" cy="0" r="6" fill="#854F0B"/><circle cx="-12" cy="-4" r="7" fill="#BA7517"/><circle cx="0" cy="-6" r="6" fill="#412402"/><circle cx="12" cy="-4" r="7" fill="#BA7517"/><circle cx="22" cy="0" r="6" fill="#854F0B"/><circle cx="22" cy="10" r="6" fill="#412402"/><circle cx="12" cy="16" r="7" fill="#BA7517"/><circle cx="0" cy="18" r="6" fill="#854F0B"/><circle cx="-12" cy="16" r="7" fill="#412402"/><circle cx="-22" cy="10" r="6" fill="#BA7517"/></g>
                </svg>
            </div>
            <div class="mt-2.5 px-1">
                <div class="flex justify-between items-baseline mb-0.5">
                    <div class="font-serif text-sm">Solaire</div>
                    <div class="text-[13px]">42 €</div>
                </div>
                <div class="text-[11px] text-secondary">Œil de tigre</div>
            </div>
        </a>

        <a href="/products/1" class="group">
            <div class="bg-bg-soft aspect-square rounded-[20px] relative overflow-hidden">
                <svg viewBox="0 0 100 100" class="w-full h-full">
                    <g transform="translate(50, 50)"><circle cx="-22" cy="0" r="6" fill="#185FA5"/><circle cx="-12" cy="-4" r="7" fill="#378ADD"/><circle cx="0" cy="-6" r="6" fill="#0C447C"/><circle cx="12" cy="-4" r="7" fill="#185FA5"/><circle cx="22" cy="0" r="6" fill="#378ADD"/><circle cx="22" cy="10" r="6" fill="#0C447C"/><circle cx="12" cy="16" r="7" fill="#185FA5"/><circle cx="0" cy="18" r="6" fill="#378ADD"/><circle cx="-12" cy="16" r="7" fill="#0C447C"/><circle cx="-22" cy="10" r="6" fill="#185FA5"/></g>
                </svg>
            </div>
            <div class="mt-2.5 px-1">
                <div class="flex justify-between items-baseline mb-0.5">
                    <div class="font-serif text-sm">Vérité</div>
                    <div class="text-[13px]">58 €</div>
                </div>
                <div class="text-[11px] text-secondary">Lapis-lazuli</div>
            </div>
        </a>

        <a href="/products/1" class="group">
            <div class="bg-bg-soft aspect-square rounded-[20px] relative overflow-hidden">
                <svg viewBox="0 0 100 100" class="w-full h-full">
                    <line x1="50" y1="22" x2="50" y2="46" stroke="#775546" stroke-width="0.5" opacity="0.5"/>
                    <ellipse cx="50" cy="60" rx="13" ry="19" fill="#1D9E75"/>
                    <ellipse cx="50" cy="60" rx="8" ry="13" fill="#5DCAA5" opacity="0.6"/>
                </svg>
            </div>
            <div class="mt-2.5 px-1">
                <div class="flex justify-between items-baseline mb-0.5">
                    <div class="font-serif text-sm">Abondance</div>
                    <div class="text-[13px]">48 €</div>
                </div>
                <div class="text-[11px] text-secondary">Aventurine</div>
            </div>
        </a>

    </div>
</section>

<footer class="px-5 md:px-8 py-6 bg-primary-dark text-bg flex flex-col md:flex-row justify-between items-center gap-2 text-xs">
    <span class="font-display text-[22px] text-accent">lithée</span>
    <span class="opacity-70">Fait main en France</span>
</footer>

</body>
</html>