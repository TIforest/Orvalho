ALTER TABLE participants ADD COLUMN achievements_visible INTEGER NOT NULL DEFAULT 0 CHECK(achievements_visible IN (0,1));
ALTER TABLE participants ADD COLUMN achievements_consent_at TEXT;
CREATE TABLE daily_references (
 participant_id TEXT NOT NULL REFERENCES participants(id),
 day TEXT NOT NULL,
 target_ml INTEGER NOT NULL CHECK(target_ml BETWEEN 700 AND 10500),
 PRIMARY KEY(participant_id,day)
);
CREATE INDEX daily_references_day ON daily_references(day,participant_id);
