ALTER TABLE participants ADD COLUMN self_withdrawn INTEGER NOT NULL DEFAULT 0 CHECK(self_withdrawn IN (0,1));
