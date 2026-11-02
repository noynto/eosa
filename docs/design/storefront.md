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
- **Sobriété** : deux couleurs d'action (`secondary`, puis `primary` au survol), pas de dégradé, pas d'ombre sur le contenu de la page.

## Motifs de la boutique

| Motif | Règles |
|---|---|
| Introduction de page | Grille photo + texte (`md:grid-cols-[1fr_1.1fr]`) ; surtitre, titre Display, deux paragraphes au plus, une action principale |
| En-tête de section | Numéro discret en serif (`01`, `02`…) suivi d'un titre Headline ; numéros dans l'ordre de la page |
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
