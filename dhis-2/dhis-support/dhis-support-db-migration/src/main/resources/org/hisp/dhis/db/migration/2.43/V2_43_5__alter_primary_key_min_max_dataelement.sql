-- Migration script to change the primary key of the minmaxdataelement table
-- Make the surrogate key (minmaxdataelementid) a sequence
ALTER TABLE minmaxdataelement
    ALTER COLUMN minmaxdataelementid SET DEFAULT nextval('hibernate_sequence');

-- Drop any rows where source, dataelement, or categoryoptioncombo is null
DELETE FROM minmaxdataelement
WHERE sourceid IS NULL
   OR dataelementid IS NULL
   OR categoryoptioncomboid IS NULL;

--  Make key columns NOT NULL
ALTER TABLE minmaxdataelement
    ALTER COLUMN sourceid SET NOT NULL,
    ALTER COLUMN dataelementid SET NOT NULL,
    ALTER COLUMN categoryoptioncomboid SET NOT NULL;

-- Drop existing PK (on minmaxdataelementid)
ALTER TABLE minmaxdataelement
    DROP CONSTRAINT IF EXISTS minmaxdataelement_pkey;

-- Drop old unique constraint (if it exists)
ALTER TABLE minmaxdataelement
    DROP CONSTRAINT IF EXISTS minmaxdataelement_unique_key;

-- Create new PRIMARY KEY on the real business key
ALTER TABLE minmaxdataelement
    ADD CONSTRAINT minmaxdataelement_pkey
    PRIMARY KEY (sourceid, dataelementid, categoryoptioncomboid);