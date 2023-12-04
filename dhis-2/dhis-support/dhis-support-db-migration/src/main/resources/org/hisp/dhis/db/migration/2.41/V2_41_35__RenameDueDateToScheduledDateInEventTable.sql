-- rename duedate in event

do $$
begin
  if exists(select 1
    from information_schema.columns
    where table_schema != 'information_schema' and table_name='event' and column_name='duedate')
  then
alter table event rename column duedate to scheduleddate;
end if;
end $$;