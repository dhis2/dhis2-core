-- rename trackedentityinstance to trackedentity
alter table if exists trackedentityinstance rename to trackedentity;

do $$
begin
  if exists(select 1
    from information_schema.columns
    where table_schema != 'information_schema' and table_name='trackedentity' and column_name='trackedentityinstanceid')
  then
alter table trackedentity rename column trackedentityinstanceid to trackedentityid;
end if;
end $$;