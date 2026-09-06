CREATE TABLE charms (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    price NUMERIC NOT NULL,
    image_id UUID REFERENCES images (id)
);

CREATE TABLE cart_item_charms (
    cart_item_id UUID NOT NULL REFERENCES cart_items (id) ON DELETE CASCADE,
    charm_id UUID NOT NULL,
    charm_name TEXT,
    charm_price NUMERIC,
    charm_image_id UUID,
    position INT NOT NULL,
    PRIMARY KEY (cart_item_id, charm_id)
);
