ALTER TABLE users
    ADD COLUMN country VARCHAR(80),
    ADD COLUMN phone_number VARCHAR(32),
    ADD COLUMN preferences_json TEXT NOT NULL DEFAULT '{}';
