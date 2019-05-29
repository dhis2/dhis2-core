
-- Remove legacy rows with no trackedentityattributeid value
delete from program_attributes where trackedentityattributeid is null;

-- Set trackedentityattributeid not null
alter table program_attributes alter column trackedentityattributeid set not null;
