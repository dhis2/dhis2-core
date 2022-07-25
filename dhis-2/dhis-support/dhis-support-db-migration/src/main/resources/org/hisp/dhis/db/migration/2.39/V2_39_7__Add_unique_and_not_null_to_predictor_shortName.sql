alter table predictor alter column shortname set not null;
alter table predictor drop constraint if exists predictor_unique_shortname;
alter table predictor add constraint predictor_unique_shortname unique (shortname);