# E-mails

L'application envoie ses e-mails depuis `contact@eosa.me` par le SMTP d'Infomaniak (Mailpit en local). Chaque e-mail a une version HTML et une version texte, produites par des templates Mustache.

## Fonctionnement

- Domaine (`mail/`) : un record `Mail(to, subject, template, model)` et l'interface `MailProvider.send(Mail)`. Les cas d'usage préparent le modèle (dates déjà en français, adresses déjà complètes) et ne voient jamais de HTML.
- Infrastructure (`infrastructure/mail/`) : `SmtpSentMails` envoie par Jakarta Mail (Angus), expéditeur affiché « EOSA », réponses vers la même adresse, délai de 10 s ; `MailTemplates` rend `templates/mail/<template>.html.mustache` et `<template>.txt.mustache`.
- Un envoi est tracé par une colonne `…_sent_at`. Un e-mail qui échoue sans bloquer l'utilisatrice est renvoyé plus tard par une tâche planifiée (voir [tâches planifiées](scheduled-tasks.md)).

## Écrire un template

- Commencer par `{{> partials/header.html.mustache}}` et finir par `{{> partials/footer.html.mustache}}` (version texte : `{{> partials/footer.txt.mustache}}`). Le nom complet avec l'extension est obligatoire : mustache.java n'ajoute pas l'extension à un nom qui contient déjà un point.
- Le contenu est une suite de lignes de tableau, chacune `<tr><td style="padding:…">` : jamais de `margin`, jamais de `border-radius`, jamais de `font-weight`.
- Couleurs de fond par l'attribut `bgcolor`, bordures dessinées par des cellules colorées, police et taille répétées sur chaque `<td>`.
- Bouton : un tableau d'une cellule `bgcolor="#601414"` contenant le lien, sans coins arrondis.
- Version texte : les valeurs en `{{{triple}}}` (sinon `&` devient `&amp;` dans les adresses web).
- Le contrôle de compatibilité HTML de Mailpit doit rester vers 95 %. Les 5 % restants (padding, line-height, body, max-width, fond) sont nécessaires à tout e-mail lisible ; 100 % n'est pas atteignable.
- Les polices de la marque ne s'affichent pas dans Gmail ni dans Outlook sur Windows : les familles de secours (Georgia, Helvetica) font partie du rendu prévu.

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
