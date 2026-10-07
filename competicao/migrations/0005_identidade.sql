-- NULL preserva cadastros legados, inclusive duplicados, sem apagar histórico.
ALTER TABLE participants ADD COLUMN identity_key TEXT;
CREATE UNIQUE INDEX participants_identity ON participants(identity_key) WHERE identity_key IS NOT NULL;
CREATE TABLE participant_aliases(token_hash TEXT PRIMARY KEY, participant_id TEXT NOT NULL REFERENCES participants(id));
