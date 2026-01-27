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