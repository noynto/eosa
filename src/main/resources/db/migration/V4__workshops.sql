CREATE TABLE workshop_venues (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    address TEXT NOT NULL,
    postal_code TEXT NOT NULL,
    city TEXT NOT NULL,
    link_url TEXT,
    image_id UUID REFERENCES images (id),
    default_capacity INT NOT NULL,
    default_duration_minutes INT NOT NULL,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE workshop_sessions (
    id UUID PRIMARY KEY,
    venue_id UUID NOT NULL REFERENCES workshop_venues (id),
    starts_at TIMESTAMPTZ NOT NULL,
    duration_minutes INT NOT NULL,
    capacity INT NOT NULL,
    state TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    cancelled_at TIMESTAMPTZ
);
CREATE INDEX workshop_sessions_starts_at_idx ON workshop_sessions (starts_at);

CREATE TABLE workshop_bookings (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES workshop_sessions (id),
    first_name TEXT NOT NULL,
    last_name TEXT NOT NULL,
    email TEXT NOT NULL,
    phone TEXT,
    state TEXT NOT NULL,
    cancel_token TEXT NOT NULL UNIQUE,
    stripe_checkout_session_id TEXT,
    stripe_payment_intent_id TEXT,
    hold_capture_before TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    confirmed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    confirmation_email_sent_at TIMESTAMPTZ,
    reminder_email_sent_at TIMESTAMPTZ,
    cancellation_email_sent_at TIMESTAMPTZ
);
-- Une seule réservation active par e-mail et par session.
CREATE UNIQUE INDEX workshop_bookings_active_email_idx
    ON workshop_bookings (session_id, LOWER(email))
    WHERE state IN ('PENDING', 'CONFIRMED');

CREATE TABLE workshop_waitlist_entries (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES workshop_sessions (id),
    first_name TEXT NOT NULL,
    email TEXT NOT NULL,
    unsubscribe_token TEXT NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_notified_at TIMESTAMPTZ,
    removed_at TIMESTAMPTZ
);
-- Une seule inscription en attente par e-mail et par session.
CREATE UNIQUE INDEX workshop_waitlist_entries_waiting_email_idx
    ON workshop_waitlist_entries (session_id, LOWER(email))
    WHERE removed_at IS NULL;
