-- add enrollment category combo column to program table
alter table program
    add column if not exists enrollmentcategorycomboid bigint;

alter table program
    drop constraint if exists fk_program_enrollmentcategorycomboid;

-- add foreign key constraint to categorycombo table
alter table program
    add constraint fk_program_enrollmentcategorycomboid
    foreign key (enrollmentcategorycomboid)
    references categorycombo(categorycomboid);

-- retrieves the default category combo by first looking for the default uid, then the default name
with default_cat_combo as (
    select coalesce(
                   (select categorycomboid from categorycombo where uid = 'bjdvmb4bfuf'),
                   (select categorycomboid from categorycombo where name = 'default')
           ) as id
)
-- sets the enrollmentcategorycomboid column to default for null values
update program
set enrollmentcategorycomboid = (select id from default_cat_combo)
where enrollmentcategorycomboid is null;

-- sets the enrollmentcategorycomboid column to not null
alter table program alter column enrollmentcategorycomboid set not null;
