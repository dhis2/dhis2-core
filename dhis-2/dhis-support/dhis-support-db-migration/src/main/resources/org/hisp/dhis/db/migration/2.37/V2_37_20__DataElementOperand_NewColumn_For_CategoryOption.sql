-- new column for categoryoption id
alter table dataelementoperand add column categoryoptionid bigint;

-- sequence for unigue id (dataelementoperand)
drop sequence if exists public.dataelementoperand_sequention;

create sequence public.dataelementoperand_sequention
    increment 1
    start 1
    minvalue 1
    maxvalue 2147483647
    cache 1;

alter sequence public.dataelementoperand_sequention  owner to dhis;

select setval('dataelementoperand_sequention', (select max(dataelementoperandid) from 	dataelementoperand));
-- sequence established

-- insert the category category option ids
insert into public.dataelementoperand(dataelementoperandid, dataelementid, categoryoptionid)
select distinct nextval('dataelementoperand_sequention'), de.dataelementid, cococ.categoryoptionid
from dataelement de
         inner join categorycombo cc
                    on  de.categorycomboid = cc.categorycomboid
         inner join categorycombos_optioncombos ccoc
                    on cc.categorycomboid = ccoc.categorycomboid
         inner join categoryoptioncombos_categoryoptions cococ
                    on ccoc.categoryoptioncomboid =  cococ.categoryoptioncomboid
         inner join dataelementoperand deo
                    on deo.dataelementid = de.dataelementid and deo.categoryoptioncomboid = ccoc.categoryoptioncomboid;

-- drop sequence
drop sequence if exists public.dataelementoperand_sequention;