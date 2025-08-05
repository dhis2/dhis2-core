-- https://dhis2.atlassian.net/browse/DHIS2-12547

UPDATE event
SET geometry = ST_SetSRID(geometry, 4326)
WHERE geometry IS NOT NULL AND ST_SRID(geometry) != 4326;

UPDATE enrollment
SET geometry = ST_SetSRID(geometry, 4326)
WHERE geometry IS NOT NULL AND ST_SRID(geometry) != 4326;

UPDATE trackedentity
SET geometry = ST_SetSRID(geometry, 4326)
WHERE geometry IS NOT NULL AND ST_SRID(geometry) != 4326;

-- Add CHECK constraints for all tables

ALTER TABLE event
    ADD CONSTRAINT enforce_event_geometry
        CHECK (
            geometry IS NULL OR
            (ST_SRID(geometry) = 4326 AND GeometryType(geometry) IN ('POINT', 'POLYGON', 'MULTIPOLYGON'))
            );

ALTER TABLE enrollment
    ADD CONSTRAINT enforce_enrollment_geometry
        CHECK (
            geometry IS NULL OR
            (ST_SRID(geometry) = 4326 AND GeometryType(geometry) IN ('POINT', 'POLYGON', 'MULTIPOLYGON'))
            );

ALTER TABLE trackedentity
    ADD CONSTRAINT enforce_trackedentity_geometry
        CHECK (
            geometry IS NULL OR
            (ST_SRID(geometry) = 4326 AND GeometryType(geometry) IN ('POINT', 'POLYGON', 'MULTIPOLYGON'))
            );
