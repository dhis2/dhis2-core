-- add empty ISO period value column
-- V2_43_10 Java script will fill it
-- V2_43_10 will make it not null
ALTER TABLE period
    ADD COLUMN IF NOT EXISTS iso varchar(50) UNIQUE;

CREATE UNIQUE INDEX IF NOT EXISTS periodtype_name_key ON periodtype (name);