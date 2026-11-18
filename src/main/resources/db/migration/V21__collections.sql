CREATE TABLE collections (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    season TEXT NOT NULL,
    year INTEGER NOT NULL,
    state TEXT NOT NULL,
    published_at TIMESTAMPTZ,
    video_id UUID REFERENCES videos (id) ON DELETE SET NULL,
    poster_image_id UUID REFERENCES images (id) ON DELETE SET NULL,
    UNIQUE (season, year)
);

CREATE TABLE collection_jewels (
    collection_id UUID NOT NULL REFERENCES collections (id) ON DELETE CASCADE,
    jewel_id UUID NOT NULL REFERENCES jewels (id) ON DELETE CASCADE,
    PRIMARY KEY (collection_id, jewel_id)
);

CREATE INDEX collection_jewels_jewel_idx ON collection_jewels (jewel_id);
