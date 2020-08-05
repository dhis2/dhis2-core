
-- Rename all AREA type to STACKED_AREA, in the "visualization" table.

UPDATE visualization SET "type"='STACKED_AREA' WHERE "type"='AREA';
