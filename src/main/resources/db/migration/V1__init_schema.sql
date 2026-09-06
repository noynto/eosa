CREATE TABLE identities (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    secret TEXT NOT NULL,
    administrator BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE UNIQUE INDEX identities_name_unique_idx ON identities (LOWER(name));

CREATE TABLE identity_sessions (
    id UUID PRIMARY KEY,
    identity_id UUID REFERENCES identities (id),
    begin_at TIMESTAMPTZ
);

CREATE TABLE jewels (
    id UUID PRIMARY KEY,
    name TEXT,
    tagline TEXT,
    price NUMERIC,
    state TEXT,
    category TEXT
);

CREATE TABLE images (
    id UUID PRIMARY KEY,
    name TEXT,
    format TEXT,
    content BYTEA NOT NULL
);

CREATE TABLE jewel_images (
    jewel_id UUID NOT NULL REFERENCES jewels (id) ON DELETE CASCADE,
    image_id UUID NOT NULL REFERENCES images (id),
    position INT NOT NULL,
    PRIMARY KEY (jewel_id, image_id)
);

CREATE TABLE carts (
    id UUID PRIMARY KEY
);

CREATE TABLE cart_items (
    cart_id UUID NOT NULL REFERENCES carts (id) ON DELETE CASCADE,
    position INT NOT NULL,
    jewel_id UUID NOT NULL,
    name TEXT,
    price NUMERIC,
    image_id UUID,
    quantity INT,
    PRIMARY KEY (cart_id, position)
);
