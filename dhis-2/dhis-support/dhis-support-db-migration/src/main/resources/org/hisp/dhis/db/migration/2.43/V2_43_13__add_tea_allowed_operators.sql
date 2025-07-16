-- Migration script to add the field allowed operators in the trackedentityattribute table
alter table trackedentityattribute
    add column if not exists allowedsearchoperators jsonb not null
        default '["EQ","GT","GE","LT","LE","LIKE","IN","SW","EW","NULL","NNULL"]'::jsonb;