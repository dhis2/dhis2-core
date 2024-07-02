ALTER TABLE dataset ADD COLUMN IF NOT EXISTS displayoptions jsonb default '{}'::jsonb;
