-- Commandes de bijoux : enregistrées à la confirmation du paiement Stripe (une commande au plus par session).
CREATE SEQUENCE order_number_seq;

CREATE TABLE orders (
    id UUID PRIMARY KEY,
    number TEXT NOT NULL UNIQUE,
    state TEXT NOT NULL,
    stripe_session_id TEXT NOT NULL UNIQUE,
    customer_name TEXT,
    customer_email TEXT,
    customer_phone TEXT,
    shipping_name TEXT,
    shipping_line1 TEXT,
    shipping_line2 TEXT,
    shipping_postal_code TEXT,
    shipping_city TEXT,
    shipping_country TEXT,
    items_amount NUMERIC(10, 2) NOT NULL,
    shipping_amount NUMERIC(10, 2) NOT NULL,
    total_amount NUMERIC(10, 2) NOT NULL,
    paid_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX orders_state_idx ON orders (state);
CREATE INDEX orders_created_at_idx ON orders (created_at);
CREATE INDEX orders_customer_email_idx ON orders (LOWER(customer_email));

CREATE TABLE order_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    position INT NOT NULL,
    name TEXT NOT NULL,
    unit_price NUMERIC(10, 2) NOT NULL,
    quantity INT NOT NULL,
    jewel_id UUID,
    image_id UUID
);
CREATE INDEX order_items_order_id_idx ON order_items (order_id, position);
