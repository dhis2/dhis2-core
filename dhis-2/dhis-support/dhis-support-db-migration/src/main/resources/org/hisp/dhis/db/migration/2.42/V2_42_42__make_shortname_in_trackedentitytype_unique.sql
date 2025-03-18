alter table trackedentitytype drop constraint if exists uk_trackedentitytype_shortname;
alter table trackedentitytype  add constraint uk_trackedentitytype_shortname unique (shortname);