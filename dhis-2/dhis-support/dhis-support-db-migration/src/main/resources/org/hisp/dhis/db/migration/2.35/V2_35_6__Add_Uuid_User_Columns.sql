

ALTER TABLE users ADD COLUMN IF NOT EXISTS "uuid" VARCHAR(36);
ALTER TABLE usergroup ADD COLUMN IF NOT EXISTS "uuid" VARCHAR(36);

UPDATE users SET uuid=gen_random_uuid() WHERE uuid IS NULL;
UPDATE usergroup SET uuid=gen_random_uuid() WHERE uuid IS NULL;
