<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Lithée — Paiement</title>
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
    <a href="/cart" class="text-[13px] text-secondary">← Retour</a>
    <a href="" class="font-display text-3xl text-primary">lithée</a>
    <span class="text-xs text-secondary hidden md:flex items-center gap-1.5">
    <svg width="13" height="13" viewBox="0 0 16 16"><path d="M8 1 L13 4 V8 C13 11 8 14 8 14 C8 14 3 11 3 8 V4 Z" fill="none" stroke="#775546" stroke-width="1.2"/></svg>
    Paiement sécurisé
  </span>
    <span class="md:hidden"></span>
</header>

<div class="px-5 md:px-8 pt-8 pb-6 flex flex-col md:flex-row gap-2 md:gap-3.5">
    <div class="flex-1 bg-bg-soft px-5 py-3.5 rounded-full flex items-center gap-3">
        <span class="w-6 h-6 rounded-full bg-bg text-primary flex items-center justify-center text-xs">✓</span>
        <span class="text-[13px] text-secondary">Panier</span>
    </div>
    <div class="flex-1 bg-bg-soft px-5 py-3.5 rounded-full flex items-center gap-3">
        <span class="w-6 h-6 rounded-full bg-bg text-primary flex items-center justify-center text-xs">✓</span>
        <span class="text-[13px] text-secondary">Livraison</span>
    </div>
    <div class="flex-1 bg-primary px-5 py-3.5 rounded-full flex items-center gap-3">
        <span class="w-6 h-6 rounded-full bg-accent text-primary flex items-center justify-center text-xs">3</span>
        <span class="text-[13px] text-accent font-medium">Paiement</span>
    </div>
</div>

<section class="px-5 md:px-8 pb-12 grid md:grid-cols-[1.5fr_1fr] gap-8">

    <form class="flex flex-col gap-6" onsubmit="event.preventDefault();">

        <div class="grid grid-cols-2 gap-3">
            <button type="button" class="bg-black text-white py-3.5 rounded-full text-[13px] flex items-center justify-center gap-2">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="#fff"><path d="M17.05 12.04c-.03-3.16 2.58-4.68 2.7-4.75-1.47-2.16-3.77-2.45-4.58-2.49-1.95-.2-3.81 1.15-4.8 1.15-1 0-2.53-1.12-4.16-1.09-2.14.03-4.11 1.24-5.21 3.16-2.22 3.86-.57 9.57 1.6 12.7 1.06 1.54 2.32 3.26 3.97 3.2 1.6-.06 2.2-1.03 4.13-1.03 1.93 0 2.47 1.03 4.16 1 1.72-.03 2.81-1.56 3.86-3.11 1.22-1.78 1.72-3.51 1.75-3.6-.04-.02-3.36-1.29-3.4-5.13zM13.91 3.4c.88-1.07 1.47-2.55 1.31-4.04-1.27.05-2.81.85-3.72 1.92-.81.94-1.52 2.45-1.33 3.91 1.42.11 2.86-.72 3.74-1.79z"/></svg>
                Apple Pay
            </button>
            <button type="button" class="bg-accent text-primary py-3.5 rounded-full text-[13px] font-medium">PayPal</button>
        </div>

        <div class="flex items-center gap-3.5 text-xs text-muted">
            <div class="flex-1 h-px bg-bg-border"></div>
            <span>ou par carte</span>
            <div class="flex-1 h-px bg-bg-border"></div>
        </div>

        <div>
            <h2 class="font-serif text-xl font-normal mb-4">Vos coordonnées</h2>
            <input type="email" placeholder="Email" value="marie.durand@email.fr" class="w-full bg-bg-soft border-0 px-4 py-3.5 rounded-2xl text-sm text-primary placeholder:text-muted">
        </div>

        <div>
            <h2 class="font-serif text-xl font-normal mb-4">Livraison</h2>
            <div class="grid grid-cols-2 gap-2.5">
                <input type="text" placeholder="Prénom" value="Marie" class="bg-bg-soft border-0 px-4 py-3.5 rounded-2xl text-sm text-primary placeholder:text-muted">
                <input type="text" placeholder="Nom" value="Durand" class="bg-bg-soft border-0 px-4 py-3.5 rounded-2xl text-sm text-primary placeholder:text-muted">
                <input type="text" placeholder="Adresse" value="14 rue des Lilas" class="col-span-2 bg-bg-soft border-0 px-4 py-3.5 rounded-2xl text-sm text-primary placeholder:text-muted">
                <input type="text" placeholder="Code postal" value="54000" class="bg-bg-soft border-0 px-4 py-3.5 rounded-2xl text-sm text-primary placeholder:text-muted">
                <input type="text" placeholder="Ville" value="Nancy" class="bg-bg-soft border-0 px-4 py-3.5 rounded-2xl text-sm text-primary placeholder:text-muted">
            </div>
        </div>

        <div>
            <h2 class="font-serif text-xl font-normal mb-4">Mode de livraison</h2>
            <div class="flex flex-col gap-2.5">
                <label class="flex items-center gap-3.5 px-5 py-4 rounded-2xl bg-accent cursor-pointer">
          <span class="w-4 h-4 rounded-full bg-bg flex-shrink-0 relative">
            <span class="absolute inset-1 rounded-full bg-primary"></span>
          </span>
                    <span class="flex-1 text-sm text-primary font-medium">Colissimo · 2-3 jours</span>
                    <span class="text-[13px] text-success font-medium">Offerte</span>
                </label>
                <label class="flex items-center gap-3.5 px-5 py-4 rounded-2xl bg-bg-soft cursor-pointer">
                    <span class="w-4 h-4 rounded-full bg-bg flex-shrink-0"></span>
                    <span class="flex-1 text-sm text-primary">Point relais · 3-5 jours</span>
                    <span class="text-[13px] text-primary">2,90 €</span>
                </label>
                <label class="flex items-center gap-3.5 px-5 py-4 rounded-2xl bg-bg-soft cursor-pointer">
                    <span class="w-4 h-4 rounded-full bg-bg flex-shrink-0"></span>
                    <span class="flex-1 text-sm text-primary">Express · 24h</span>
                    <span class="text-[13px] text-primary">9,90 €</span>
                </label>
            </div>
        </div>

        <div>
            <h2 class="font-serif text-xl font-normal mb-4">Paiement</h2>

            <div class="bg-bg-soft rounded-2xl p-4 flex flex-col gap-2.5">
                <input type="text" placeholder="Numéro de carte" value="•••• •••• •••• 4521" class="w-full bg-bg border-0 px-4 py-3.5 rounded-xl text-sm text-primary tracking-widest placeholder:text-muted">
                <div class="grid grid-cols-2 gap-2.5">
                    <input type="text" placeholder="MM / AA" class="bg-bg border-0 px-4 py-3.5 rounded-xl text-sm text-primary placeholder:text-muted">
                    <input type="text" placeholder="CVV" class="bg-bg border-0 px-4 py-3.5 rounded-xl text-sm text-primary placeholder:text-muted">
                </div>
            </div>

            <label class="flex items-center gap-3.5 px-5 py-3.5 rounded-2xl bg-bg border border-bg-border mt-2.5 cursor-pointer">
                <span class="bg-accent text-primary px-2.5 py-1 rounded-full text-[11px] font-medium">Klarna</span>
                <span class="flex-1 text-[13px] text-secondary">Payer en 3× · 50,67 €</span>
            </label>
        </div>

    </form>

    <aside>
        <div class="bg-bg-soft p-6 rounded-3xl md:sticky md:top-5">
            <div class="font-serif text-xl mb-5">Votre commande</div>

            <div class="flex flex-col gap-3.5 mb-5">

                <div class="grid grid-cols-[56px_1fr_auto] gap-3 items-center">
                    <div class="bg-bg aspect-square rounded-xl">
                        <svg viewBox="0 0 56 56" class="w-full h-full">
                            <g transform="translate(28, 28)"><circle cx="-12" cy="0" r="4" fill="#7F77DD"/><circle cx="-6" cy="-3" r="5" fill="#AFA9EC"/><circle cx="0" cy="-4" r="4" fill="#534AB7"/><circle cx="6" cy="-3" r="5" fill="#7F77DD"/><circle cx="12" cy="0" r="4" fill="#AFA9EC"/><circle cx="12" cy="6" r="4" fill="#534AB7"/><circle cx="6" cy="9" r="5" fill="#7F77DD"/><circle cx="0" cy="10" r="4" fill="#AFA9EC"/><circle cx="-6" cy="9" r="5" fill="#534AB7"/><circle cx="-12" cy="6" r="4" fill="#7F77DD"/></g>
                        </svg>
                    </div>
                    <div>
                        <div class="font-serif text-sm">Sérénité</div>
                        <div class="text-[11px] text-secondary">×1 · 17 cm</div>
                    </div>
                    <div class="text-[13px]">38 €</div>
                </div>

                <div class="grid grid-cols-[56px_1fr_auto] gap-3 items-center">
                    <div class="bg-bg aspect-square rounded-xl">
                        <svg viewBox="0 0 56 56" class="w-full h-full">
                            <line x1="28" y1="6" x2="28" y2="26" stroke="#775546" stroke-width="0.5"/>
                            <circle cx="28" cy="34" r="9" fill="#ED93B1"/><circle cx="28" cy="34" r="6" fill="#F4C0D1"/><circle cx="28" cy="34" r="3" fill="#D4537E"/>
                        </svg>
                    </div>
                    <div>
                        <div class="font-serif text-sm">Tendresse</div>
                        <div class="text-[11px] text-secondary">×1 · 45 cm</div>
                    </div>
                    <div class="text-[13px]">52 €</div>
                </div>

                <div class="grid grid-cols-[56px_1fr_auto] gap-3 items-center">
                    <div class="bg-bg aspect-square rounded-xl">
                        <svg viewBox="0 0 56 56" class="w-full h-full">
                            <g transform="translate(28, 28)"><circle cx="-12" cy="0" r="4" fill="#854F0B"/><circle cx="-6" cy="-3" r="5" fill="#BA7517"/><circle cx="0" cy="-4" r="4" fill="#412402"/><circle cx="6" cy="-3" r="5" fill="#BA7517"/><circle cx="12" cy="0" r="4" fill="#854F0B"/><circle cx="12" cy="6" r="4" fill="#412402"/><circle cx="6" cy="9" r="5" fill="#BA7517"/><circle cx="0" cy="10" r="4" fill="#854F0B"/><circle cx="-6" cy="9" r="5" fill="#412402"/><circle cx="-12" cy="6" r="4" fill="#BA7517"/></g>
                        </svg>
                    </div>
                    <div>
                        <div class="font-serif text-sm">Solaire <span class="bg-accent px-1.5 py-px rounded-full text-[9px] font-sans ml-0.5">Sur-mesure</span></div>
                        <div class="text-[11px] text-secondary">×1 · « M ♡ J »</div>
                    </div>
                    <div class="text-[13px]">62 €</div>
                </div>
            </div>

            <div class="flex flex-col gap-2 text-[13px] text-secondary pt-4 border-t border-[#D9CFB8]">
                <div class="flex justify-between"><span>Sous-total</span><span class="text-primary">152 €</span></div>
                <div class="flex justify-between"><span>Livraison</span><span class="text-success font-medium">Offerte</span></div>
            </div>

            <div class="flex justify-between items-baseline py-4 border-t border-[#D9CFB8] my-4">
                <span class="text-sm">Total</span>
                <span class="font-serif text-3xl">152 €</span>
            </div>

            <button class="w-full bg-primary text-accent py-4 rounded-full text-sm font-medium">Confirmer la commande</button>

            <div class="mt-3.5 flex items-center gap-1.5 text-[11px] text-secondary justify-center">
                <svg width="11" height="11" viewBox="0 0 16 16"><path d="M8 1 L13 4 V8 C13 11 8 14 8 14 C8 14 3 11 3 8 V4 Z" fill="none" stroke="#775546" stroke-width="1"/></svg>
                Transaction chiffrée SSL
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