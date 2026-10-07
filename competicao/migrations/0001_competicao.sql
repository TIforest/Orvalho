CREATE TABLE invites (
 hash TEXT PRIMARY KEY, expected_name TEXT NOT NULL, sector TEXT NOT NULL,
 expires_at TEXT NOT NULL, revoked INTEGER NOT NULL DEFAULT 0
);
CREATE TABLE participants (
 id TEXT PRIMARY KEY, invite_hash TEXT NOT NULL UNIQUE REFERENCES invites(hash),
 token_hash TEXT NOT NULL UNIQUE, name TEXT NOT NULL, sector TEXT NOT NULL,
 joined_at TEXT NOT NULL, active INTEGER NOT NULL DEFAULT 1, consent_version TEXT NOT NULL
);
CREATE TABLE events (
 participant_id TEXT NOT NULL REFERENCES participants(id), id TEXT NOT NULL,
 occurred_at TEXT NOT NULL, day TEXT NOT NULL, ml INTEGER NOT NULL CHECK(ml BETWEEN 10 AND 2000),
 canceled INTEGER NOT NULL DEFAULT 0 CHECK(canceled IN (0,1)),
 PRIMARY KEY(participant_id,id)
);
CREATE INDEX events_day ON events(day,participant_id,canceled);
CREATE TABLE rate_limits (bucket TEXT PRIMARY KEY, count INTEGER NOT NULL, expires INTEGER NOT NULL);
