-- rename tei in potentialduplicate

do $$
begin
  if exists(select 1
    from information_schema.columns
    where table_schema != 'information_schema' and table_name='potentialduplicate' and column_name='teia')
  then
alter table potentialduplicate rename column teia to original;
end if;
end $$;

do $$
begin
  if exists(select 1
    from information_schema.columns
    where table_schema != 'information_schema' and table_name='potentialduplicate' and column_name='teib')
  then
alter table potentialduplicate rename column teib to duplicate;
end if;
end $$;