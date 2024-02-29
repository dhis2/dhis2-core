ALTER TABLE section ADD COLUMN IF NOT EXISTS textbeforesection text;
ALTER TABLE section ADD COLUMN IF NOT EXISTS textaftersection text;
ALTER TABLE section ADD COLUMN IF NOT EXISTS displayoptions jsonb default '{}'::jsonb;

