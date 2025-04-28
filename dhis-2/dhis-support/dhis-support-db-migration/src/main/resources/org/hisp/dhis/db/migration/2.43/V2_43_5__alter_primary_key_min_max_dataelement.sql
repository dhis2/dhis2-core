ALTER TABLE minmaxdataelement
    ALTER COLUMN minmaxdataelementid SET DEFAULT nextval('hibernate_sequence');

-- Step 2: Make key columns NOT NULL
ALTER TABLE minmaxdataelement
    ALTER COLUMN sourceid SET NOT NULL,
    ALTER COLUMN dataelementid SET NOT NULL,
    ALTER COLUMN categoryoptioncomboid SET NOT NULL;

-- Step 3: Drop existing PK (on minmaxdataelementid)
ALTER TABLE minmaxdataelement
    DROP CONSTRAINT IF EXISTS minmaxdataelement_pkey;

-- Step 4: Drop old unique constraint (if it exists)
ALTER TABLE minmaxdataelement
    DROP CONSTRAINT IF EXISTS minmaxdataelement_unique_key;

-- Step 5: Create new PRIMARY KEY on the real business key
ALTER TABLE minmaxdataelement
    ADD CONSTRAINT minmaxdataelement_pkey
    PRIMARY KEY (sourceid, dataelementid, categoryoptioncomboid);