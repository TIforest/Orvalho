-- Preserva participantes, tokens e historico. Novos cadastros nao exigem convite.
PRAGMA defer_foreign_keys = ON;
CREATE TABLE participants_password (
 id TEXT PRIMARY KEY, invite_hash TEXT UNIQUE REFERENCES invites(hash),
 token_hash TEXT NOT NULL UNIQUE, name TEXT NOT NULL, sector TEXT NOT NULL,
 joined_at TEXT NOT NULL, active INTEGER NOT NULL DEFAULT 1, consent_version TEXT NOT NULL
);
INSERT INTO participants_password(id,invite_hash,token_hash,name,sector,joined_at,active,consent_version)
 SELECT id,invite_hash,token_hash,name,sector,joined_at,active,consent_version FROM participants;
DROP TABLE participants;
ALTER TABLE participants_password RENAME TO participants;
PRAGMA defer_foreign_keys = OFF;
