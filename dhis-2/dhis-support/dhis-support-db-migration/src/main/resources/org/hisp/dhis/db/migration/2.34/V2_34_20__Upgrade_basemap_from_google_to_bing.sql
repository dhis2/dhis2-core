
-- Upgrade basemap field in map table from Google to Bing equivalents

update map set basemap = 'bingLight' where basemap = 'googleStreets';
update map set basemap = 'bingHybrid' where basemap = 'googleHybrid';
