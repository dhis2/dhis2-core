ALTER TABLE trackedentityattribute
    ADD COLUMN IF NOT EXISTS allowedsearchtypes jsonb
    default '["EQ","GT","GE","LT","LE","LIKE","IN","SW","EW","IEQ","NE","NIEQ","NLIKE","ILIKE","NILIKE"]'::jsonb;