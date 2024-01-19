-- rename enddate in enrollment

do $$
begin
  if exists(select 1
    from information_schema.columns
    where table_schema != 'information_schema' and table_name='enrollment' and column_name='incidentdate')
  then
alter table enrollment rename column incidentdate to occurreddate;
end if;
end $$;