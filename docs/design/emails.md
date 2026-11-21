# Design system : les e-mails

Les e-mails reprennent le design de la [boutique](storefront.md) et les [fondations](foundations.md) : mêmes couleurs, mêmes familles de polices, mêmes arrondis, avec le niveau d'expression de Material 3 Expressive (formes arrondies, boutons en pilule, surfaces tonales). Un e-mail n'a pas accès aux jetons Tailwind : les valeurs ci-dessous sont les hexadécimaux des jetons, répétés dans les gabarits. Choix et raisons : [spécification](../specs/email-design.md). Règles d'écriture du HTML : [E-mails](../tech/emails.md).

## Ce que l'e-mail assume de perdre

Gmail et Outlook pour Windows ne chargent pas les polices web ; Outlook pour Windows ignore les `border-radius`. Le design en tient compte au lieu de s'y plier :

- **Logo** : une image PNG (Bagel Fat One), le seul élément garanti identique partout où les images s'affichent ; texte alternatif « eosa. » en Georgia quand elles sont bloquées.
- **Polices** : Cormorant Garamond et DM Sans par `@font-face` (Apple Mail, iOS Mail…) ; Georgia et Helvetica ailleurs. Aucun texte ne dépend d'une police de la marque pour rester lisible.
- **Formes** : cartes, encadrés et boutons arrondis ; angles droits dans Outlook pour Windows, sans que la mise en page casse.

## Valeurs

| Rôle | Jeton | Valeur |
|---|---|---|
| Fond de page | `bg-soft` | `#F4EFE5` |
| Carte, fond du logo | `bg` | `#FDFBF7` |
| Contour, séparateur | `bg-border` | `#F0E6D6` |
| Titre, logo | `primary` | `#601414` |
| Texte | `primary-dark` | `#3D0A0A` |
| Texte secondaire, surtitre | `secondary` | `#775546` |
| Mention, texte discret | `muted` | `#7B6A4E` |
| Bouton (fond) | `secondary` | `#775546` |
| Bouton (texte) | `bg` | `#FDFBF7` |

Contrastes : voir [fondations](foundations.md#contrastes-mesurés) ; le bouton (`bg` sur `secondary`) fait 6,4:1.

## Éléments

| Élément | Règle |
|---|---|
| Carte | 560 px au plus, fond `#FDFBF7`, contour de 1 px `#F0E6D6`, rayon 24 px, marge de 16 px autour sur mobile. Fournie par l'en-tête et le pied. |
| Logo | PNG de 224 × 72 px affiché en 112 × 36 px, fond `#FDFBF7`, `alt="eosa."`, lien vers le site. Fourni par l'en-tête. |
| Surtitre | DM Sans 12 px, interligne 18 px, espacement des lettres 0,16 em, majuscules, `#775546`. |
| Titre | Cormorant Garamond 30 px, interligne 34 px, graisse 400, `#601414`. |
| Texte | DM Sans 15 px, interligne 24 px, `#3D0A0A`. |
| Encadré | Tableau de fond `#F4EFE5`, rayon 16 px, 20 px de marge intérieure ; son titre en Cormorant Garamond 20 px. Pour les informations clés : lieu, date, récapitulatif. |
| Bouton | Cellule `bgcolor="#775546"`, rayon 24 px, lien `inline-block` de 14 px × 28 px de marge intérieure et 20 px d'interligne (48 px de haut), DM Sans 15 px graisse 500, texte `#FDFBF7`, sans soulignement. Une seule action principale par e-mail. |
| Chiffres importants | Montants, total, numéros de suivi et de commande en **DM Sans** (graisse 500 pour les mettre en avant), jamais en Cormorant Garamond : ses chiffres elzéviriens, de hauteurs variables, rendent « 00581108 » ou « 64,30 € » difficiles à lire. |
| Note | DM Sans 14 px, interligne 22 px, `#775546` : une information utile qui n'est pas l'action principale (délai de confirmation, annulation, désinscription). Les liens y sont en `#601414`, soulignés. |
| Mention | DM Sans 12 px, interligne 19 px, `#7B6A4E` : mention sans conséquence (« ce message n'a été envoyé que pour tester »). |
| Lien dans un texte | Couleur `#601414`, **soulignement conservé**. |

Le gabarit `test.html.mustache` montre chacun de ces éléments : c'est le modèle à recopier.

## Accessibilité

- Contrastes AA ; texte de 15 px au moins pour le corps, 12 px au plus petit.
- `lang="fr"`, `role="presentation"` sur les tableaux de mise en page, texte alternatif sur toute image.
- Lien souligné dans un texte, bouton de 48 px de haut au moins.
- Version texte (`*.txt.mustache`) pour chaque e-mail.

## Compatibilité visée

Le contrôle de compatibilité HTML de Mailpit reste à **90 % de prise en charge ou plus** (92 % mesuré le 21 novembre 2026 sur le gabarit de test). `@font-face` (peu pris en charge) et `border-radius` (partiel) sont les écarts voulus.
