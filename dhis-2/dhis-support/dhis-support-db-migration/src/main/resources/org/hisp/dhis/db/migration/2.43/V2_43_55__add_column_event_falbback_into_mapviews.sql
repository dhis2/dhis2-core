-- Related to https://dhis2.atlassian.net/browse/DHIS2-20416

alter table mapview add column if not exists eventCoordinateFieldFallback character varying(15);
