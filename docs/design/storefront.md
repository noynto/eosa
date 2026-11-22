# Design system : la boutique

La boutique applique les [fondations](foundations.md) avec un niveau d'expression **fondamental** au sens de Material 3 Expressive : les jetons, les états et l'accessibilité, mais ni formes qui se transforment, ni couleurs vives, ni animations à ressort. C'est la vitrine d'un artisan bijoutier : les photos des bijoux et des lieux sont le sujet, l'interface s'efface derrière elles.

## Ce qu'on prend à Material, ce qu'on laisse

| On prend | On laisse |
|---|---|
| Rôles de couleur, échelles de typographie et d'arrondis, durées d'animation | L'esthétique « Google » : couleurs saturées, formes arrondies partout, typographie Roboto |
| États d'interaction (survol, focus, désactivé) et accessibilité | Les animations à ressort et les formes qui se transforment |
| Hiérarchie des actions : une action principale par écran, des actions secondaires plus discrètes | Les boutons flottants (FAB), barres d'outils et menus déroulants d'application |

## Direction

- **Éditoriale** : grands titres en serif (Cormorant Garamond), surtitre en petites capitales espacées, beaucoup d'espace blanc, fond crème `bg`.
- **L'image d'abord** : photos grandes, sans cadre ni ombre, recadrées en `object-cover` à un format fixe (`aspect-[4/3]` pour une carte, pleine hauteur pour une introduction) ; zoom de 2 % au survol, 500 ms au plus.
- **Sobriété** : deux couleurs d'action (`secondary`, puis `primary` au survol), pas de dégradé, pas d'ombre sur le contenu de la page. Seule exception : le voile du hero de l'accueil (voir les motifs).

## Motifs de la boutique

| Motif | Règles |
|---|---|
| Introduction de page | Grille photo + texte (`md:grid-cols-[1fr_1.1fr]`) ; surtitre, titre Display, deux paragraphes au plus, une action principale |
| Hero de l'accueil | Plein écran (`min-h-hero-screen`, 85 % de la hauteur visible) : vidéo ou photo en fond (`object-cover`), texte crème en bas à gauche posé sur un voile `from-primary-dark/75 from-60% to-transparent`, qui garantit un contraste d'au moins 4,5:1 sur n'importe quelle image (mesuré à 8,6:1 sur la photo la plus claire). Texte en HTML, une action principale (pilule crème) et une secondaire (pilule à contour crème), bouton lecture / pause de 44 px s'il y a une vidéo |
| Bande éditoriale | Bande pleine largeur coupée en deux (`grid md:grid-cols-2 border-t`), photo d'un côté, texte de l'autre, une action : « L'atelier » ; « Un cadeau » remplace la photo par une carte stylisée sur fond `bg-soft`, faute de photo (à remplacer par une photo si l'on en a une) |
| Image agrandie | Bouton « Zoom » (44 px de haut) sur la photo d'une fiche bijou, et clic sur la photo : fenêtre native `<dialog>` plein écran sur un fond `primary-dark/95`, image en `object-contain`, bouton de fermeture de 44 px ; elle se ferme avec Échap, par le bouton ou en cliquant dessus, et rend le focus au bouton « Zoom ». Sans photo, pas de bouton |
| En-tête de section | Numéro discret en serif (`01`, `02`…) suivi d'un titre Headline ; numéros dans l'ordre de la page. Une bande éditoriale (voir ci-dessus) n'est pas numérotée : elle ouvre par un surtitre et un grand titre, et la suite des numéros continue après elle (01 Nos univers, 02 Nouveautés, bandes, 03 Prochains ateliers) |
| Choix d'une option (fiche bijou) | Un `fieldset` par choix, titre en `legend` serif (`text-xl`), sans numéro. Métal : pastilles rondes de 56 px portant la photo du métal, libellé dessous ; choisie = anneau `primary` décalé de 2 px (`ring-offset-bg`), libellé en `font-medium`. Breloques : cartes de 64 px (`rounded-2xl`, contour `outline`) avec une case de 20 px ; choisie = fond `bg-soft` et coche, sans bordure. Au repos, contour `outline` (3:1) ; survol, contour `secondary` ; focus, le contour de la règle de base reporté sur la pastille ou la carte (`peer-focus-visible`). Les vrais champs sont en `sr-only`, jamais `hidden`. Un encart « Votre sélection » (`bg-soft`, `aria-live="polite"`) donne le résumé et le prix total, rechargé par htmx ; le bouton « Ajouter au panier » n'affiche pas le montant |
| Carte (bijou, session d'atelier) | Image en tête, texte dessous (`p-5`), `rounded-2xl`, contour `bg-border` ; l'action en bas de carte |
| Action principale | Bouton pilule `rounded-full bg-secondary text-bg px-6 py-3.5 text-sm font-medium`, survol `bg-primary` ; une seule par écran ou par carte |
| Action secondaire | Pilule à contour `rounded-full border border-bg-border text-secondary`, survol `border-secondary` ; ou lien souligné `underline underline-offset-4` |
| Champ de formulaire | `bg-bg border rounded-xl px-4 py-3 text-sm`, étiquette `text-xs text-secondary` au-dessus, aide en dessous ; contour au jeton `outline` |
| Message après une action | Encadré `rounded-xl border px-4 py-3 text-sm` : `error` avec `role="alert"`, `success` avec `role="status"` ; placé au-dessus du contenu concerné |
| Page de résultat (confirmation, annulation) | Centrée, pastille d'icône, titre Headline, une phrase, un récapitulatif en carte, une action |
| Chargement d'un fragment htmx | Squelette `animate-pulse` à la forme de la carte attendue |

## À éviter

- Plus d'une action principale par écran.
- Du texte posé sur une photo (lisibilité incertaine) ; si c'est indispensable, sur un voile `primary/30` au moins.
- Des icônes décoratives sans rôle ; une icône accompagne un texte, sauf convention évidente (panier, Instagram).
- Les émojis dans l'interface, hors textes de la marque.
