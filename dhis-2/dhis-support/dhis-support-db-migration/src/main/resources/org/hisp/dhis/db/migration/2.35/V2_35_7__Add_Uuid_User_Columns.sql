

ALTER TABLE users ADD COLUMN IF NOT EXISTS "uuid" uuid;
ALTER TABLE usergroup ADD COLUMN IF NOT EXISTS "uuid" uuid;

UPDATE users SET uuid=gen_random_uuid() WHERE uuid IS NULL;
UPDATE usergroup SET uuid=gen_random_uuid() WHERE uuid IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uuid_users_idx ON users (uuid);
CREATE UNIQUE INDEX IF NOT EXISTS uuid_usergroup_idx ON usergroup (uuid);
