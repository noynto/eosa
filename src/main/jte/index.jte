<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Lithée — Boutique</title>
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

<section class="px-5 md:px-8 pt-10 pb-6 flex flex-col md:flex-row md:justify-between md:items-end gap-4">
    <div>
        <h1 class="font-serif text-4xl md:text-5xl font-normal leading-none">La collection</h1>
        <div class="text-sm text-secondary mt-3">42 pièces</div>
    </div>
    <div class="bg-bg-soft px-4 py-2.5 rounded-full text-[13px] text-secondary flex items-center gap-2 cursor-pointer self-start md:self-auto">
        Trier : Nouveautés
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#775546" stroke-width="2"><path d="m6 9 6 6 6-6"/></svg>
    </div>
</section>

<div class="px-5 md:px-8 pb-6 flex gap-2 flex-wrap">
    <span class="bg-primary text-accent px-5 py-2.5 rounded-full text-[13px] cursor-pointer">Tous</span>
    <span class="bg-bg-soft text-primary px-5 py-2.5 rounded-full text-[13px] cursor-pointer">Bracelets</span>
    <span class="bg-bg-soft text-primary px-5 py-2.5 rounded-full text-[13px] cursor-pointer">Colliers</span>
    <span class="bg-bg-soft text-primary px-5 py-2.5 rounded-full text-[13px] cursor-pointer">Bagues</span>
    <span class="bg-bg-soft text-primary px-5 py-2.5 rounded-full text-[13px] cursor-pointer">Boucles</span>
    <span class="bg-accent text-primary px-5 py-2.5 rounded-full text-[13px] cursor-pointer flex items-center gap-1.5">
    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#601414" stroke-width="2"><path d="M3 6h18"/><path d="M7 12h10"/><path d="M10 18h4"/></svg>
    Filtrer
  </span>
</div>

<section class="px-5 md:px-8 pb-12 grid grid-cols-2 md:grid-cols-3 gap-4 md:gap-5">

    <a href="/products/1" class="group">
        <div class="bg-bg-soft aspect-square rounded-3xl relative overflow-hidden">
            <svg viewBox="0 0 200 200" class="w-full h-full">
                <ellipse cx="100" cy="135" rx="58" ry="9" fill="#775546" opacity="0.2"/>
                <g transform="translate(100, 100)">
                    <circle cx="-50" cy="0" r="13" fill="#7F77DD"/><circle cx="-28" cy="-9" r="15" fill="#AFA9EC"/><circle cx="-3" cy="-13" r="13" fill="#534AB7"/><circle cx="22" cy="-11" r="15" fill="#7F77DD"/><circle cx="44" cy="-2" r="13" fill="#AFA9EC"/><circle cx="50" cy="20" r="14" fill="#534AB7"/><circle cx="30" cy="34" r="13" fill="#7F77DD"/><circle cx="0" cy="38" r="14" fill="#AFA9EC"/><circle cx="-30" cy="34" r="13" fill="#534AB7"/><circle cx="-50" cy="20" r="14" fill="#7F77DD"/>
                </g>
            </svg>
            <button class="absolute top-3.5 right-3.5 w-9 h-9 bg-bg rounded-full flex items-center justify-center" aria-label="Favoris">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#601414" stroke-width="2"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.29 1.51 4.04 3 5.5l7 7Z"/></svg>
            </button>
        </div>
        <div class="mt-3.5 px-1">
            <div class="flex justify-between items-baseline mb-1">
                <div class="font-serif text-[17px]">Sérénité</div>
                <div class="text-[15px] font-medium">38 €</div>
            </div>
            <div class="text-xs text-secondary">Améthyste</div>
        </div>
    </a>

    <a href="/products/1" class="group">
        <div class="bg-accent aspect-square rounded-3xl relative overflow-hidden">
            <svg viewBox="0 0 200 200" class="w-full h-full">
                <line x1="100" y1="30" x2="100" y2="100" stroke="#775546" stroke-width="0.6" opacity="0.5"/>
                <circle cx="100" cy="120" r="36" fill="#ED93B1"/>
                <circle cx="100" cy="120" r="24" fill="#F4C0D1"/>
                <circle cx="100" cy="120" r="12" fill="#D4537E"/>
            </svg>
            <div class="absolute top-3.5 left-3.5 bg-primary text-accent px-3 py-1 rounded-full text-[11px]">Nouveau</div>
            <button class="absolute top-3.5 right-3.5 w-9 h-9 bg-bg rounded-full flex items-center justify-center" aria-label="Favoris">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#601414" stroke-width="2"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.29 1.51 4.04 3 5.5l7 7Z"/></svg>
            </button>
        </div>
        <div class="mt-3.5 px-1">
            <div class="flex justify-between items-baseline mb-1">
                <div class="font-serif text-[17px]">Tendresse</div>
                <div class="text-[15px] font-medium">52 €</div>
            </div>
            <div class="text-xs text-secondary">Quartz rose</div>
        </div>
    </a>

    <a href="/products/1" class="group">
        <div class="bg-bg-soft aspect-square rounded-3xl relative overflow-hidden">
            <svg viewBox="0 0 200 200" class="w-full h-full">
                <ellipse cx="100" cy="135" rx="58" ry="9" fill="#775546" opacity="0.2"/>
                <g transform="translate(100, 100)">
                    <circle cx="-48" cy="0" r="13" fill="#854F0B"/><circle cx="-28" cy="-9" r="15" fill="#BA7517"/><circle cx="-3" cy="-13" r="13" fill="#412402"/><circle cx="20" cy="-11" r="15" fill="#BA7517"/><circle cx="42" cy="-2" r="13" fill="#854F0B"/><circle cx="48" cy="20" r="14" fill="#412402"/><circle cx="28" cy="34" r="13" fill="#BA7517"/><circle cx="0" cy="38" r="14" fill="#854F0B"/><circle cx="-28" cy="34" r="13" fill="#412402"/><circle cx="-48" cy="20" r="14" fill="#BA7517"/>
                </g>
            </svg>
            <button class="absolute top-3.5 right-3.5 w-9 h-9 bg-bg rounded-full flex items-center justify-center" aria-label="Favoris">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#601414" stroke-width="2"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.29 1.51 4.04 3 5.5l7 7Z"/></svg>
            </button>
        </div>
        <div class="mt-3.5 px-1">
            <div class="flex justify-between items-baseline mb-1">
                <div class="font-serif text-[17px]">Solaire</div>
                <div class="text-[15px] font-medium">42 €</div>
            </div>
            <div class="text-xs text-secondary">Œil de tigre</div>
        </div>
    </a>

    <a href="/products/1" class="group">
        <div class="bg-bg-soft aspect-square rounded-3xl relative overflow-hidden">
            <svg viewBox="0 0 200 200" class="w-full h-full">
                <ellipse cx="100" cy="135" rx="58" ry="9" fill="#775546" opacity="0.2"/>
                <g transform="translate(100, 100)">
                    <circle cx="-50" cy="0" r="13" fill="#185FA5"/><circle cx="-28" cy="-9" r="15" fill="#378ADD"/><circle cx="-3" cy="-13" r="13" fill="#0C447C"/><circle cx="22" cy="-11" r="15" fill="#185FA5"/><circle cx="44" cy="-2" r="13" fill="#378ADD"/><circle cx="50" cy="20" r="14" fill="#0C447C"/><circle cx="30" cy="34" r="13" fill="#185FA5"/><circle cx="0" cy="38" r="14" fill="#378ADD"/><circle cx="-30" cy="34" r="13" fill="#0C447C"/><circle cx="-50" cy="20" r="14" fill="#185FA5"/>
                </g>
            </svg>
            <button class="absolute top-3.5 right-3.5 w-9 h-9 bg-bg rounded-full flex items-center justify-center" aria-label="Favoris">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="#601414" stroke="#601414" stroke-width="2"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.29 1.51 4.04 3 5.5l7 7Z"/></svg>
            </button>
        </div>
        <div class="mt-3.5 px-1">
            <div class="flex justify-between items-baseline mb-1">
                <div class="font-serif text-[17px]">Vérité</div>
                <div class="text-[15px] font-medium">58 €</div>
            </div>
            <div class="text-xs text-secondary">Lapis-lazuli</div>
        </div>
    </a>

    <a href="/products/1" class="group">
        <div class="bg-bg-soft aspect-square rounded-3xl relative overflow-hidden">
            <svg viewBox="0 0 200 200" class="w-full h-full">
                <line x1="100" y1="40" x2="100" y2="92" stroke="#775546" stroke-width="0.6" opacity="0.5"/>
                <ellipse cx="100" cy="120" rx="26" ry="38" fill="#1D9E75"/>
                <ellipse cx="100" cy="120" rx="16" ry="26" fill="#5DCAA5" opacity="0.6"/>
            </svg>
            <button class="absolute top-3.5 right-3.5 w-9 h-9 bg-bg rounded-full flex items-center justify-center" aria-label="Favoris">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#601414" stroke-width="2"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.29 1.51 4.04 3 5.5l7 7Z"/></svg>
            </button>
        </div>
        <div class="mt-3.5 px-1">
            <div class="flex justify-between items-baseline mb-1">
                <div class="font-serif text-[17px]">Abondance</div>
                <div class="text-[15px] font-medium">48 €</div>
            </div>
            <div class="text-xs text-secondary">Aventurine</div>
        </div>
    </a>

    <a href="/products/1" class="group">
        <div class="bg-bg-soft aspect-square rounded-3xl relative overflow-hidden">
            <svg viewBox="0 0 200 200" class="w-full h-full">
                <ellipse cx="100" cy="135" rx="58" ry="9" fill="#775546" opacity="0.2"/>
                <g transform="translate(100, 100)">
                    <circle cx="-50" cy="0" r="13" fill="#601414"/><circle cx="-28" cy="-9" r="15" fill="#3D0A0A"/><circle cx="-3" cy="-13" r="13" fill="#601414"/><circle cx="22" cy="-11" r="15" fill="#3D0A0A"/><circle cx="44" cy="-2" r="13" fill="#601414"/><circle cx="50" cy="20" r="14" fill="#3D0A0A"/><circle cx="30" cy="34" r="13" fill="#601414"/><circle cx="0" cy="38" r="14" fill="#3D0A0A"/><circle cx="-30" cy="34" r="13" fill="#601414"/><circle cx="-50" cy="20" r="14" fill="#3D0A0A"/>
                </g>
            </svg>
            <div class="absolute top-3.5 left-3.5 bg-primary text-accent px-3 py-1 rounded-full text-[11px]">−15%</div>
            <button class="absolute top-3.5 right-3.5 w-9 h-9 bg-bg rounded-full flex items-center justify-center" aria-label="Favoris">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#601414" stroke-width="2"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.29 1.51 4.04 3 5.5l7 7Z"/></svg>
            </button>
        </div>
        <div class="mt-3.5 px-1">
            <div class="flex justify-between items-baseline mb-1">
                <div class="font-serif text-[17px]">Grenat</div>
                <div class="text-[15px] font-medium">
                    <span class="text-muted line-through text-xs mr-1 font-normal">55€</span>47 €
                </div>
            </div>
            <div class="text-xs text-secondary">Passion</div>
        </div>
    </a>

</section>

<div class="px-5 md:px-8 pt-6 pb-10 flex justify-center items-center gap-2">
    <span class="w-9 h-9 rounded-full bg-primary text-accent flex items-center justify-center text-[13px] cursor-pointer">1</span>
    <span class="w-9 h-9 rounded-full bg-bg-soft text-secondary flex items-center justify-center text-[13px] cursor-pointer">2</span>
    <span class="w-9 h-9 rounded-full bg-bg-soft text-secondary flex items-center justify-center text-[13px] cursor-pointer">3</span>
    <span class="w-9 h-9 rounded-full bg-bg-soft text-secondary flex items-center justify-center cursor-pointer">→</span>
</div>

<footer class="px-5 md:px-8 py-6 bg-primary-dark text-bg flex flex-col md:flex-row justify-between items-center gap-2 text-xs">
    <span class="font-display text-[22px] text-accent">lithée</span>
    <span class="opacity-70">Fait main en France</span>
</footer>

</body>
</html>