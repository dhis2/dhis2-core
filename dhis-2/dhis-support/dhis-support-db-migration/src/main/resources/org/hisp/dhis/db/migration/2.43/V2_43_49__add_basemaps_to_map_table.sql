-- Migration related to DHIS2-20417.

alter table map add column if not exists "basemaps" jsonb default '[]'::jsonb;

update map set basemaps = json_build_array(json_build_object('id', basemap, 'opacity', 1, 'hidden', false))::jsonb;
