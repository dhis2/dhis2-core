ALTER TABLE trackedentityattribute ADD COLUMN IF NOT EXISTS allowedsearchtypes jsonb default '[]'::jsonb;

UPDATE trackedentityattribute SET allowedsearchtypes = '["EQ","GT","GE","LT","LE","LIKE","IN","SW","EW"]'::jsonb where allowedsearchtypes = '[]';