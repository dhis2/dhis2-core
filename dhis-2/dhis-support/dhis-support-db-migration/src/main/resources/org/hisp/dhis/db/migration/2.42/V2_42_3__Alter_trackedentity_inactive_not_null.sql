-- DHIS2-16442: set trackedentity's inactive column to not null and default to false
update trackedentity set inactive = false where inactive is null;
alter table trackedentity alter column inactive set not null, alter column inactive set default false;