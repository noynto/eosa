-- Date à laquelle la liste d'attente a été prévenue de l'ouverture des réservations (une seule fois par session).
ALTER TABLE workshop_sessions ADD COLUMN opening_announced_at TIMESTAMPTZ;
