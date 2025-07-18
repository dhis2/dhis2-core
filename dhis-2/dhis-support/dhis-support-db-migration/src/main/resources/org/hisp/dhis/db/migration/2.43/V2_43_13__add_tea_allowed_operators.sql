-- Migration script to add the field blocked search operators in the trackedentityattribute table
alter table trackedentityattribute add column if not exists blockedsearchoperators jsonb default null;