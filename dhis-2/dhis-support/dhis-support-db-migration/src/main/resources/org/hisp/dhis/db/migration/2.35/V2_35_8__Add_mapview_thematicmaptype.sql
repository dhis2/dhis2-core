
-- Add column 'thematicmaptype' to 'mapview', set to default value and set not null

alter table mapview add column if not exists "thematicmaptype" varchar(50);
update mapview set "thematicmaptype" = 'CHOROPLETH' where "thematicmaptype" is null and "layer" like 'thematic%';
