-- add 'geometry' column using Spatial Ref System 4326 (http://spatialreference.org/ref/epsg/4326/)
alter table orgunitgroup
add column geometry geometry(Geometry, 4326);

-- Drop obsolete columns
alter table orgunitgroup
drop column coordinates;
