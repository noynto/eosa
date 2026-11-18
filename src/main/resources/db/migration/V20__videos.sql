CREATE TABLE videos (
    id UUID PRIMARY KEY,
    name TEXT,
    format TEXT NOT NULL,
    size INTEGER NOT NULL,
    content BYTEA NOT NULL
);

-- Du H.264 ne se compresse pas : stockage hors ligne sans compression, pour lire une plage par substring()
-- sans charger tout le fichier (mesuré : environ 1 ms pour 256 Kio sur 8 Mio).
ALTER TABLE videos ALTER COLUMN content SET STORAGE EXTERNAL;
