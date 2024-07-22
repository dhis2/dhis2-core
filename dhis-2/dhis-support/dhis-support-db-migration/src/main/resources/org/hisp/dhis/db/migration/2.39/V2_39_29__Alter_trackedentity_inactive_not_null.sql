-- DHIS2-16442: set trackedentity's inactive column to not null and default to false
update trackedentityinstance set inactive = false where inactive is null;
alter table trackedentityinstance alter column inactive set not null, alter column inactive set default false;