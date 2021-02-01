
-- Add column 'thematicmaptype' to 'mapview'

alter table mapview add column if not exists "thematicmaptype" varchar(50);

-- Upgrade existing thematic map views to 'CHOROPLETH'

update mapview set "thematicmaptype" = 'CHOROPLETH' where "thematicmaptype" is null and "layer" like 'thematic%';
