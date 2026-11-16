-- Cartes cadeau : le solde est la somme des mouvements du journal (jamais stocké). Les montants sont
-- toujours positifs ; le sens dépend du type de mouvement. expires_at est exclusif (minuit, heure de Paris,
-- du lendemain du dernier jour de validité).
CREATE TABLE gift_cards (
    id UUID PRIMARY KEY,
    code TEXT NOT NULL,
    token TEXT NOT NULL,
    state TEXT NOT NULL,
    initial_amount NUMERIC(10, 2) NOT NULL,
    buyer_first_name TEXT,
    buyer_email TEXT,
    email_sent_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ NOT NULL,
    stripe_session_id TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX gift_cards_code_idx ON gift_cards (code);
CREATE UNIQUE INDEX gift_cards_token_idx ON gift_cards (token);

CREATE TABLE gift_card_movements (
    id UUID PRIMARY KEY,
    gift_card_id UUID NOT NULL REFERENCES gift_cards (id) ON DELETE CASCADE,
    kind TEXT NOT NULL,
    amount NUMERIC(10, 2) NOT NULL,
    checkout_session_id TEXT,
    reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX gift_card_movements_card_idx ON gift_card_movements (gift_card_id, created_at);
