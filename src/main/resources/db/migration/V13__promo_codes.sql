-- Codes de promotion gérés par EOSA. starts_at est inclusif, ends_at est exclusif (minuit, heure de Paris,
-- du lendemain du dernier jour de validité). Le montant minimum porte sur les bijoux, avant remise.
CREATE TABLE promo_codes (
    id UUID PRIMARY KEY,
    code TEXT NOT NULL,
    type TEXT NOT NULL,
    value NUMERIC(10, 2) NOT NULL,
    starts_at TIMESTAMPTZ,
    ends_at TIMESTAMPTZ,
    max_uses INT,
    minimum_amount NUMERIC(10, 2),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX promo_codes_code_idx ON promo_codes (code);
