-- Effacement de l'adresse de livraison et du téléphone d'une commande au terme de leur durée de conservation :
-- la date de l'effacement est tracée, les montants et les lignes sont gardés.
ALTER TABLE orders
    ADD COLUMN personal_data_purged_at TIMESTAMPTZ;
