# Design system : fondations

Le design d'EOSA suit la méthode de Material 3 (Expressive) sans en utiliser les composants : pas de bibliothèque ni de composant web JavaScript, seulement du HTML rendu par Mustache, des classes Tailwind et quelques lignes de script quand c'est indispensable. Material fournit les règles (rôles de couleur, échelles, états, accessibilité) ; la marque fournit les valeurs. Ces fondations sont communes à la boutique ([application à la boutique](storefront.md)) et à l'administration ([application à l'administration](admin.md)).

Principes :

- **Minimalisme** : un élément ne s'ajoute que s'il sert la lecture ou l'action ; pas de décoration sans rôle.
- **Jetons uniquement** : couleurs, tailles de texte, arrondis et ombres viennent des jetons ci-dessous (déclarés dans `src/main/resources/public/tailwind.config.js`) ; jamais de valeur arbitraire (`text-[13px]`, `rounded-[20px]`, `red-600`).
- **Accessible par défaut** : contrastes WCAG AA, focus toujours visible, cibles tactiles confortables, messages annoncés aux lecteurs d'écran.

## Couleurs

Chaque couleur a un rôle Material ; on choisit une couleur par son rôle, jamais par son apparence.

| Rôle Material | Jeton Tailwind | Valeur | Usage |
|---|---|---|---|
| Primary | `primary` | `#601414` | Couleur de la marque : texte principal, titres, actions principales de l'administration, logo |
| Primary (survol, appui) | `primary-dark` | `#3D0A0A` | Survol et appui des actions `primary` ; fond des voiles (`primary-dark/40`) |
| On primary | `accent` | `#FFEFAE` | Texte et icônes posés sur `primary` (administration) |
| Secondary | `secondary` | `#775546` | Texte secondaire, actions principales de la boutique, liens |
| On secondary / on primary (boutique) | `bg` | `#FDFBF7` | Texte posé sur `secondary` ou `primary` dans la boutique |
| Surface | `bg` | `#FDFBF7` | Fond des pages et des cartes |
| Surface container | `bg-soft` | `#F4EFE5` | Fond des zones en retrait : encadrés, champs, zones d'image vides |
| Outline variant | `bg-border` | `#F0E6D6` | Séparateurs et contours décoratifs (cartes, sections) |
| Outline | `outline` *(à créer)* | `#9D8764` | Contour des champs de formulaire et des contrôles (contraste 3:1) |
| On surface variant (texte discret) | `muted` *(à corriger)* | `#7B6A4E` (au lieu de `#B8A88E`) | Texte d'aide, mentions, dates : lisible (4,5:1) |
| Décoratif | `muted` actuel `#B8A88E` | — | À réserver aux éléments décoratifs non textuels (numéros de section, puces) une fois le texte passé sur la nouvelle valeur |
| Success | `success` *(à corriger)* | `#188260` (au lieu de `#1D9E75`) | Confirmations (« Sauvegardé », pastille de réussite) |
| Error | `error` *(à créer)* | `#B3261E` | Messages d'erreur et actions destructives ; remplace les `red-300` à `red-700` |
| Scrim | `primary/30` | — | Voile derrière une fenêtre modale |

### Contrastes mesurés

| Texte | Fond | Contraste | Verdict WCAG AA |
|---|---|---|---|
| `primary` | `bg` | 12,6:1 | Conforme |
| `secondary` | `bg` / `bg-soft` | 6,4:1 / 5,8:1 | Conforme |
| `accent` | `primary` | 11,3:1 | Conforme |
| `bg` | `secondary` / `primary` | 6,4:1 / 12,6:1 | Conforme |
| `muted` actuel `#B8A88E` | `bg` / `bg-soft` | 2,3:1 / 2,0:1 | **Insuffisant** pour du texte |
| `muted` corrigé `#7B6A4E` | `bg` / `bg-soft` | 5,1:1 / 4,6:1 | Conforme |
| `success` actuel `#1D9E75` | `bg` | 3,3:1 | Insuffisant pour du petit texte |
| `success` corrigé `#188260` | `bg` | 4,6:1 | Conforme |
| `error` `#B3261E` | `bg` | 6,3:1 | Conforme |
| Contour `bg-border` | `bg` | 1,2:1 | Décoratif seulement : insuffisant pour un champ |
| Contour `outline` `#9D8764` | `bg` / `bg-soft` | 3,3:1 / 3,0:1 | Conforme pour un contrôle (3:1) |

Règle : du texte doit atteindre 4,5:1 (3:1 au-delà de 24 px, ou 19 px en gras) ; le contour d'un champ, d'une case ou d'un bouton sans fond doit atteindre 3:1.

## Typographie

| Rôle Material | Police | Classes | Usage |
|---|---|---|---|
| Logo | Bagel Fat One | `font-display` | Le mot « eosa. » uniquement |
| Display | Cormorant Garamond | `font-serif font-normal text-5xl md:text-6xl leading-[1.05]` | Titre principal d'une page de la boutique |
| Headline | Cormorant Garamond | `font-serif font-normal text-3xl` à `text-4xl` | Titres de section, titres de page secondaires |
| Title | Cormorant Garamond (boutique) / DM Sans `font-medium` (administration) | `text-xl` à `text-2xl` (boutique), `text-sm font-medium` (administration) | Titres de carte, de bloc |
| Body | DM Sans | `text-sm leading-relaxed` | Texte courant (14 px) |
| Label | DM Sans | `text-xs`, `font-medium` pour un bouton | Boutons, étiquettes de champ, métadonnées (12 px minimum) |
| Overline | DM Sans | `text-xs tracking-widest uppercase` | Surtitre au-dessus d'un titre (« ATELIERS ») |

Règles : 12 px (`text-xs`) est la taille minimale ; pas de taille arbitraire ; deux graisses seulement (`font-normal` 400, `font-medium` 500, les seules chargées par `fonts.css`) ; le serif ne sert qu'aux titres.

## Forme

| Jeton Material | Classe | Rayon | Usage |
|---|---|---|---|
| None | `rounded-none` | 0 | Images pleine largeur, bandeaux |
| Extra small | `rounded` | 4 px | Petites pastilles, cases |
| Small | `rounded-lg` | 8 px | Contrôles de l'administration : boutons, champs, menus |
| Medium | `rounded-xl` | 12 px | Cartes de l'administration, champs de la boutique, alertes |
| Large | `rounded-2xl` | 16 px | Cartes et encadrés de la boutique |
| Extra large | `rounded-3xl` | 24 px | Grandes surfaces de la boutique (fenêtres, blocs mis en avant) |
| Full | `rounded-full` | — | Boutons et pastilles de la boutique, badges, avatars |

Une famille de rayons par contexte : un même écran n'empile pas plus de deux rayons différents.

## Élévation

L'interface est plate : la profondeur vient des surfaces (`bg` / `bg-soft`) et des contours (`bg-border`), pas des ombres. Une ombre (`shadow-lg`) est réservée à ce qui flotte au-dessus de la page : fenêtre modale, boîte de confirmation, menu ouvert.

## Mouvement

- Transition par défaut : `transition-colors` (150 ms) sur tout élément interactif.
- Courbe Material standard pour une transition plus longue : `cubic-bezier(0.2, 0, 0, 1)`.
- Durées : 150 ms pour un changement d'état, 300 ms pour une apparition, 500 ms au plus (zoom léger d'une image au survol).
- Chargement : squelettes `animate-pulse` à la forme du contenu attendu, plutôt qu'un indicateur qui tourne.
- Pas de ressorts ni de formes qui se transforment (le côté « Expressive » qui demanderait du JavaScript).
- Toute animation respecte la préférence système de réduction des mouvements (`motion-reduce:transition-none`, `motion-reduce:animate-none`).

## États d'interaction

| État | Règle |
|---|---|
| Survol | Changement de couleur du rôle (`secondary` → `primary`, `primary` → `primary-dark`, contour `bg-border` → `secondary`) |
| Focus clavier | Toujours visible : `focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-secondary`. Un `outline-none` n'est permis que s'il est remplacé par un autre indicateur aussi visible |
| Appui | Couleur de survol, sans déplacement de l'élément |
| Désactivé | `opacity-40 cursor-not-allowed`, et l'attribut `disabled` ; la raison est écrite à côté quand elle n'est pas évidente |
| Erreur | Contour et message en `error`, message relié au champ |

## Accessibilité

- Contrastes : voir le tableau ci-dessus.
- Cibles tactiles : 48 px de haut recommandés par Material ; 44 px minimum pour un bouton ou un lien isolé, sur mobile comme sur ordinateur.
- Chaque champ a un `<label>` ; chaque image a un `alt` qui décrit le bijou ou le lieu (vide pour une image décorative).
- Les messages qui apparaissent après une action portent `role="alert"` (erreur) ou `role="status"` (confirmation).
- La langue de la page (`lang="fr"`) et l'ordre de lecture suivent l'ordre visuel.

## Espacement et mise en page

- Grille de 4 px de Tailwind ; espacements courants : `gap-2`, `gap-3`, `gap-4`, `p-5`, `p-6`.
- Marges latérales des sections : `px-5 md:px-8` ; rythme vertical : `py-12 md:py-16`.
- Largeur de lecture : `max-w-md` à `max-w-xl` pour un texte, `max-w-5xl` pour une grille.
- Trois tailles d'écran (mobile < 640 px, tablette 640–1024 px, ordinateur ≥ 1024 px) : voir [web](../tech/web.md#responsive).

## Écarts relevés dans le code (novembre 2026)

Le relevé des templates montre ce qui reste à mettre en conformité :

- [ ] `text-muted` utilisé pour du texte (22 fois dans la boutique, 43 dans l'administration) avec un contraste de 2,3:1 → passer `muted` à `#7B6A4E`.
- [ ] Focus clavier invisible : 43 champs en `focus:outline-none` avec seulement un changement de contour, aucun style de focus sur les boutons et les liens.
- [ ] Contour des champs en `bg-border` (1,2:1) → jeton `outline`.
- [ ] `bg-error` utilisé sur la page de paiement refusé, mais la couleur `error` n'existe pas dans la configuration : la pastille n'a pas de fond.
- [ ] Rouges en dur (`red-300`, `red-600`, `red-700`) au lieu d'un jeton `error`.
- [ ] `text-success` en petit texte (« Sauvegardé ») à 3,3:1 → passer `success` à `#188260`.
- [ ] Tailles arbitraires : `text-[13px]` (14 fois, boutique), `text-[11px]` et `text-[10px]` (12 fois, administration) ; `rounded-[20px]` (1 fois).
- [ ] Boutons de l'administration de 28 px de haut (`py-1.5 text-xs`) : sous la cible de 44 px.
