# Design system : l'administration

L'administration applique les [fondations](foundations.md) avec un niveau d'expression **excellent** au sens de Material 3 Expressive : les composants et motifs Material, des états d'interaction complets et la couleur de la marque comme couleur principale, toujours sans composant JavaScript. Les formes qui se transforment et les animations à ressort (niveau « transformationnel ») sont exclues. C'est un outil de travail : clarté, densité raisonnable et rapidité d'action priment.

## Composants Material et leur équivalent

| Composant Material | Équivalent dans l'administration | Règles |
|---|---|---|
| Navigation drawer / rail | Colonne latérale (`aside`, ordinateur) + barre d'onglets défilante (mobile) dans `header-admin` | Rubriques groupées sous un surtitre (« Catalogue », « Ateliers ») ; rubrique active à signaler (`bg-bg-soft text-primary`) |
| Top app bar | En-tête : logo, mention « Admin », lien « Voir le site » | Toujours visible, sans action métier |
| Filled button | `bg-primary text-accent rounded-lg px-4 py-2.5 text-sm font-medium`, survol `bg-primary-dark` | Action principale d'un écran ou d'un bloc : une seule par bloc |
| Outlined button | `border border-bg-border rounded-lg text-secondary`, survol `border-secondary` | Action secondaire (« Archiver ce lieu ») |
| Text button | Lien souligné `text-secondary underline underline-offset-4` | Action discrète dans une ligne de liste (« Annuler ») |
| Bouton destructif | Outlined button avec survol `error`, et toujours une confirmation (`hx-confirm`) | Annuler une session, marquer absente |
| Card | Bloc `bg-bg border border-bg-border rounded-xl p-5` | Un bloc = un sujet = son propre formulaire et son enregistrement (comme les fiches bijou et lieu) |
| List | Lignes `py-3 border-b border-bg-border last:border-0` | Texte principal `text-sm font-medium`, secondaire `text-xs text-secondary`, actions à droite (en dessous sur mobile) |
| Text field (outlined) | `bg-bg-soft border rounded-lg px-3 py-2.5 text-sm`, étiquette `text-xs text-secondary` au-dessus | Contour au jeton `outline`, focus visible, message d'erreur dessous |
| Dialog | Fenêtre modale (`fixed inset-0 bg-primary/30`) pour créer un élément ; boîte de confirmation de `footer-admin` pour `hx-confirm` | Titre, contenu court, actions alignées à droite (annuler à gauche de confirmer) |
| Snackbar | Message court dans le bloc concerné (« Sauvegardé ») | `role="status"`, couleur `success` ; une erreur reste affichée en `error` avec `role="alert"` |
| Chip / Badge | Pastille `rounded-full border text-xs px-2` (« Archivé », « Annulée ») | Information d'état, jamais une action |
| Progress indicator | Squelette `animate-pulse` | À la forme des lignes ou cartes attendues |
| FAB | Non utilisé | L'action « + Nouveau… » est un filled button en haut à droite de la liste |

## Densité et cibles

- Les contrôles font au moins 44 px de haut sur mobile (`py-2.5` avec `text-sm`, ou `min-h-11`) ; sur ordinateur, 36 px sont acceptés dans une liste dense.
- Les tableaux deviennent des listes empilées sous 640 px : rien ne doit défiler horizontalement.

## Couleur et expression

- La couleur principale est le bordeaux de la marque (`primary`), avec `accent` pour le texte posé dessus : l'administration se reconnaît comme EOSA sans couleurs supplémentaires.
- Les états (survol, focus, appui, désactivé) suivent les [fondations](foundations.md#états-dinteraction) sur chaque élément interactif.
- Les réponses htmx suivent les conventions de [web](../tech/web.md#htmx) : `HX-Refresh` ou `HX-Redirect` en cas de succès, fragment 422 en `error` en cas de refus.
