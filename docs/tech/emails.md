# E-mails

L'application envoie ses e-mails depuis `contact@eosa.me` par le SMTP d'Infomaniak (Mailpit en local). Chaque e-mail a une version HTML et une version texte, produites par des templates Mustache.

## Fonctionnement

- Domaine (`mail/`) : un record `Mail(to, subject, template, model)` et l'interface `MailProvider.send(Mail)`. Les cas d'usage préparent le modèle (dates déjà en français, adresses déjà complètes) et ne voient jamais de HTML.
- Infrastructure (`infrastructure/mail/`) : `SmtpSentMails` envoie par Jakarta Mail (Angus), expéditeur affiché « EOSA », réponses vers la même adresse, délai de 10 s ; `MailTemplates` rend `templates/mail/<template>.html.mustache` et `<template>.txt.mustache`.
- Un envoi est tracé par une colonne `…_sent_at`. Un e-mail qui échoue sans bloquer l'utilisatrice est renvoyé plus tard par une tâche planifiée (voir [tâches planifiées](scheduled-tasks.md)).

## Écrire un template

- Commencer par `{{> partials/header.html.mustache}}` et finir par `{{> partials/footer.html.mustache}}` (version texte : `{{> partials/footer.txt.mustache}}`). Le nom complet avec l'extension est obligatoire : mustache.java n'ajoute pas l'extension à un nom qui contient déjà un point.
- Le design (couleurs, polices, formes, éléments) est décrit dans [Design : e-mails](../design/emails.md), et le gabarit `test.html.mustache` le montre en entier : s'en inspirer.
- Le contenu est une suite de lignes de tableau, chacune `<tr><td style="padding:…">`, sans `margin` (Outlook l'ignore). `border-radius` et `font-weight` (400 ou 500) sont permis : Outlook pour Windows les ignore sans casser la mise en page.
- Couleurs de fond par l'attribut `bgcolor` (avec `border-radius` dans le `style` pour arrondir), police et taille répétées sur chaque `<td>`, `role="presentation"` sur les tableaux de mise en page.
- Bouton : un tableau d'une cellule `bgcolor="#775546"` (`border-radius:24px`, `mso-padding-alt:14px 28px`) contenant un lien `display:inline-block;padding:14px 28px`.
- Chaque modèle reçoit `baseUrl` (adresse publique du site) et `logoUrl` de `MailTemplates` : ne pas les ajouter dans `WorkshopMails` ni dans les autres services d'e-mail. Les adresses `/fonts/…` et `/mail/logo.png` sont celles des fichiers statiques du site.
- Version texte : les valeurs en `{{{triple}}}` (sinon `&` devient `&amp;` dans les adresses web).
- Le contrôle de compatibilité HTML de Mailpit doit rester à **90 % de prise en charge ou plus**. Les écarts voulus sont `@font-face`, `border-radius`, `overflow` et `<style>` ; 100 % n'est pas atteignable.
- Les polices de la marque ne s'affichent pas dans Gmail ni dans Outlook sur Windows : les familles de secours (Georgia, Helvetica) font partie du rendu prévu. Le logo est une image, donc identique partout où les images s'affichent.

## Regénérer le logo

Le logo `src/main/resources/public/mail/logo.png` (224 × 72 px, « eosa. » en Bagel Fat One sur le fond de la carte) vient de `scripts/mail-logo.js`. Pour le refaire, par exemple si la couleur ou la police de la marque change, avec Chromium dans Docker :

```bash
mkdir -p /tmp/mail-logo && chmod 777 /tmp/mail-logo
docker run --rm -v "$PWD":/repo:ro -v /tmp/mail-logo:/out ghcr.io/puppeteer/puppeteer:latest sh -c \
  "NODE_PATH=\$(npm root -g) node /repo/scripts/mail-logo.js \
   /repo/src/main/resources/public/fonts/hYkPPucsQOr5dy02WmQr5Zkd4Blsvw.woff2 /out/logo.png"
cp /tmp/mail-logo/logo.png src/main/resources/public/mail/logo.png
```

Les images d'un e-mail sont chargées par la messagerie depuis `eosa.me` : le fichier doit rester servi par la boutique.

## Tester

- En local, `docker compose up` envoie tout vers Mailpit : http://localhost:8025 (contenu, rendu, contrôle de compatibilité, clic sur les liens).
- `docker compose run --rm send-test-email` envoie un e-mail de test (variable `EOSA_SEND_TEST_EMAIL_TO`).
- Pour tester le vrai SMTP sans écrire le mot de passe dans un fichier ni dans l'historique du terminal :

```bash
read -s -p "Mot de passe SMTP : " EOSA_SMTP_PASSWORD; echo; export EOSA_SMTP_PASSWORD
EOSA_SMTP_HOST=mail.infomaniak.com EOSA_SMTP_PORT=465 EOSA_SMTP_SECURITY=ssl \
EOSA_SMTP_USERNAME=contact@eosa.me EOSA_SEND_TEST_EMAIL_TO=vous@exemple.fr \
docker compose run --rm send-test-email
unset EOSA_SMTP_PASSWORD
```

## Délivrabilité

Le domaine `eosa.me` publie SPF (`include:spf.infomaniak.ch -all`) et DMARC (`p=reject`) : un e-mail mal authentifié est rejeté. N'envoyer que par le SMTP d'Infomaniak, et vérifier dans « Afficher l'original » de Gmail que SPF, DKIM et DMARC sont à PASS après un changement de configuration.

Variables `EOSA_SMTP_*` et `EOSA_MAIL_FROM` : voir [déploiement](../ops/deployment.md).
