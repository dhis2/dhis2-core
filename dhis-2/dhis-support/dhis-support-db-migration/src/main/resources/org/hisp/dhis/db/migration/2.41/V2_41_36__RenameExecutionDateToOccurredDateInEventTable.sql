-- rename executiondate in event

do $$
begin
  if exists(select 1
    from information_schema.columns
    where table_schema != 'information_schema' and table_name='event' and column_name='executiondate')
  then
alter table event rename column executiondate to occurreddate;
end if;
end $$;