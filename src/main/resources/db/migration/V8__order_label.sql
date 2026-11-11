-- Fabrication d'une commande : étiquette d'expédition du transporteur et date de fabrication.
ALTER TABLE orders
    ADD COLUMN crafted_at TIMESTAMPTZ,
    ADD COLUMN carrier TEXT,
    ADD COLUMN tracking_number TEXT,
    ADD COLUMN parcel_weight_grams INT,
    ADD COLUMN label_reference TEXT,
    ADD COLUMN label_status TEXT;
