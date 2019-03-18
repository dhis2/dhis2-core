-- add 'geometry' column using Spatial Ref System 4326 (http://spatialreference.org/ref/epsg/4326/)
alter table organisationunit
add column geometry geometry(Geometry, 4326);

-- update the 'geometry' field with existing data from 'coordinates' column (Point type)
update organisationunit
set geometry = ST_SetSRID(ST_GeomFromGeoJSON('{"type":"Point", "coordinates":' || coordinates || '}'), 4326)
where featuretype = 'POINT';

-- update the 'geometry' field with existing data from 'coordinates' column (Polygon/Multipolygon type)
update organisationunit
set geometry = ST_SetSRID(ST_GeomFromGeoJSON('{"type":"MultiPolygon", "coordinates":' || coordinates || '}'), 4326)
where featuretype in ('POLYGON', 'MULTI_POLYGON');

-- This query fixes the issue by translating MultiPolygons with only one geometry to a Polygon
update organisationunit ou
set geometry = sq.geom
from (
  select organisationunitid, (ST_DUMP(geometry)).geom
  from organisationunit
  where ST_GeometryType(geometry) = 'ST_MultiPolygon'
  and ST_NumGeometries(geometry) = 1
) sq
where ou.organisationunitid = sq.organisationunitid;

-- Drop obsolete columns
alter table organisationunit
drop column coordinates;

alter table organisationunit
drop column featuretype;
