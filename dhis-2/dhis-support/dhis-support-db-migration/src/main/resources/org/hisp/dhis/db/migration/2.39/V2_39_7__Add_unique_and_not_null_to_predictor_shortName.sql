alter table predictor alter column shortname set not null;
alter table predictor add constraint predictor_unique_shortname unique (shortname);