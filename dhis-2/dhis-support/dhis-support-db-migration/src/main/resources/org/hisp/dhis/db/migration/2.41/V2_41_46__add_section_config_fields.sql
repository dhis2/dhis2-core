ALTER TABLE section ADD COLUMN IF NOT EXISTS displayoptions jsonb default '{}'::jsonb;

