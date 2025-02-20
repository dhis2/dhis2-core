
-- Creates an index if it does not already exist.
-- @param index_name the name of the index.
-- @param index_sql the SQL statement for creating the index.
-- @return true if the index was created, or false if it already exists.
create or replace function dhis2_create_index_if_not_exists(index_name text, index_sql text)
returns boolean as $$
begin
  -- Check if index already exists in public schema
  if not exists (
    select 1
    from pg_class c
    inner join pg_namespace n on n.oid = c.relnamespace
    where c.relname = index_name and n.nspname = 'public'
    ) then
      -- Create index if it does not exist
      execute index_sql;
      return true;
    else
      return false;
    end if;
end;
$$ language plpgsql;
