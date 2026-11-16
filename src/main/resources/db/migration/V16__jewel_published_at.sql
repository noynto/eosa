-- Date de première publication d'un bijou : elle ordonne les nouveautés (les plus récentes d'abord). Jusqu'ici
-- les bijoux étaient triés par identifiant, un UUID aléatoire sans ordre chronologique. Les bijoux déjà publiés
-- prennent la date de cette migration : leur vraie date de publication n'a jamais été enregistrée.
ALTER TABLE jewels ADD COLUMN published_at TIMESTAMPTZ;

UPDATE jewels SET published_at = NOW() WHERE state = 'PUBLISHED';
