-- Expédition, livraison et annulation d'une commande : dates de chaque étape, motif d'annulation et envoi de
-- l'e-mail d'expédition à la cliente.
ALTER TABLE orders
    ADD COLUMN shipped_at TIMESTAMPTZ,
    ADD COLUMN delivered_at TIMESTAMPTZ,
    ADD COLUMN cancelled_at TIMESTAMPTZ,
    ADD COLUMN cancel_reason TEXT,
    ADD COLUMN shipping_sent_at TIMESTAMPTZ;
