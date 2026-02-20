-- Migration related to DHIS2-20845.

alter table periodtype add column if not exists label character varying(230);
