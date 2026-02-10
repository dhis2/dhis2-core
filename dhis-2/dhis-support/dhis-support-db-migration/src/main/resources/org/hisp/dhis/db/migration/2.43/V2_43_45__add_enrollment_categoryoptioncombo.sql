-- add enrollment category combo column to the enrollment table
alter table enrollment
    add column if not exists attributeoptioncomboid bigint;

alter table enrollment
    drop constraint if exists fk_enrollment_categorycomboid;

-- add foreign key constraint to categorycombo table
alter table enrollment
    add constraint fk_enrollment_categorycomboid
    foreign key (attributeoptioncomboid)
    references categoryoptioncombo(categoryoptioncomboid);

-- Retrieves the default cat opt combo by first looking for the default UID, then the default name for older databases
with default_cat_opt_combo as (
    select coalesce ((
                         select "categoryoptioncomboid" from categoryoptioncombo where uid = 'HllvX50cXC0'), (
                         select "categoryoptioncomboid" from categoryoptioncombo where name = 'default')) as id
)

-- Sets the attributeoptioncomboid column to default for null values
update enrollment
set attributeoptioncomboid = (select id from default_cat_opt_combo)
where attributeoptioncomboid is null;

-- Sets the attributeoptioncomboid column to not null
alter table enrollment alter column attributeoptioncomboid set not null;
