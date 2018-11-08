--Add jsonb value columns to datastore and user datastore
ALTER TABLE keyjsonvalue ADD COLUMN IF NOT EXISTS jbvalue jsonb;
ALTER TABLE userkeyjsonvalue ADD COLUMN IF NOT EXISTS jbvalue jsonb;


--Migrate existing values into jsonb column
--NOT IDEMPOTENT
UPDATE keyjsonvalue SET jbvalue = value::jsonb where jbvalue is null;
UPDATE userkeyjsonvalue SET jbvalue = value::jsonb where jbvalue is null;


--Delete old value column
ALTER TABLE keyjsonvalue DROP COLUMN IF EXISTS value;
ALTER TABLE userkeyjsonvalue DROP COLUMN IF EXISTS value;
