
-- Remove legacy rows with no dataelementid value
delete from programstagedataelement where dataelementid is null;

-- Set trackedentityattributeid not null
alter table programstagedataelement alter column dataelementid set not null;
