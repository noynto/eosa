-- Remise accordée à la commande par un code de promotion : le code est photographié (promo_code) comme les prix,
-- promo_code_id relie la commande à son code pour compter les usages. La clé étrangère interdit de supprimer
-- un code qui a servi.
ALTER TABLE orders
    ADD COLUMN promo_code_id UUID REFERENCES promo_codes (id),
    ADD COLUMN promo_code TEXT,
    ADD COLUMN discount_amount NUMERIC(10, 2) NOT NULL DEFAULT 0;

CREATE INDEX orders_promo_code_id_idx ON orders (promo_code_id);
