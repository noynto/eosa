CREATE TABLE metal_colors (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    image_id UUID REFERENCES images (id)
);

-- cart_items passe d'une PK composite (cart_id, position) à un id propre,
-- pour que chaque ligne (bijou + couleur) soit adressable individuellement.
ALTER TABLE cart_items ADD COLUMN id UUID;
UPDATE cart_items SET id = gen_random_uuid() WHERE id IS NULL;
ALTER TABLE cart_items ALTER COLUMN id SET NOT NULL;
ALTER TABLE cart_items DROP CONSTRAINT cart_items_pkey;
ALTER TABLE cart_items ADD PRIMARY KEY (id);
CREATE INDEX cart_items_cart_id_idx ON cart_items (cart_id);

ALTER TABLE cart_items ADD COLUMN metal_color_id UUID;
ALTER TABLE cart_items ADD COLUMN metal_color_name TEXT;
ALTER TABLE cart_items ADD COLUMN metal_color_image_id UUID;
