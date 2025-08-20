-- https://dhis2.atlassian.net/browse/DHIS2-19965

ALTER TABLE event
ALTER COLUMN geometry TYPE geometry(Geometry, 4326)
  USING CASE
         WHEN geometry IS NULL THEN NULL
         WHEN ST_SRID(geometry) = 0 THEN ST_SetSRID(geometry, 4326)
         WHEN ST_SRID(geometry) = 4326 THEN geometry
         ELSE ST_Transform(geometry, 4326)
END;

ALTER TABLE enrollment
ALTER COLUMN geometry TYPE geometry(Geometry, 4326)
  USING CASE
         WHEN geometry IS NULL THEN NULL
         WHEN ST_SRID(geometry) = 0 THEN ST_SetSRID(geometry, 4326)
         WHEN ST_SRID(geometry) = 4326 THEN geometry
         ELSE ST_Transform(geometry, 4326)
END;

ALTER TABLE trackedentity
ALTER COLUMN geometry TYPE geometry(Geometry, 4326)
  USING CASE
         WHEN geometry IS NULL THEN NULL
         WHEN ST_SRID(geometry) = 0 THEN ST_SetSRID(geometry, 4326)
         WHEN ST_SRID(geometry) = 4326 THEN geometry
         ELSE ST_Transform(geometry, 4326)
END;