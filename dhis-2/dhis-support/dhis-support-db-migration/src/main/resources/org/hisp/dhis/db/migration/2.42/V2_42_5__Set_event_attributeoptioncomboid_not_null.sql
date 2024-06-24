
-- Retrieves the default cat opt combo by first looking for the default UID, then the default name for older databases
with default_cat_opt_combo as (
select coalesce ((
  select "categoryoptioncomboid" from "categoryoptioncombo" where "uid" = 'HllvX50cXC0'), (
  select "categoryoptioncomboid" from "categoryoptioncombo" where "name" = 'default')) as id
)
-- Sets the attributeoptioncomboid column to default for null values
update "event"
set "attributeoptioncomboid" = (select id from default_cat_opt_combo)
where "attributeoptioncomboid" is null;

-- Sets the attributeoptioncomboid column to not null
alter table "event" alter column "attributeoptioncomboid" set not null;
