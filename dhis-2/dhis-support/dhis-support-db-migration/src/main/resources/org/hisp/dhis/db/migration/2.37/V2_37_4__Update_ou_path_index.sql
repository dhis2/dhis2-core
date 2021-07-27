drop index if exists in_organisationunit_path;
create index if not exists in_organisationunit_path on organisationunit using btree(path varchar_pattern_ops);