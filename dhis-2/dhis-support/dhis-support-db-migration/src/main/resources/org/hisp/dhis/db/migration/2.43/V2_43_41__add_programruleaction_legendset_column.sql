-- Migration related to DHIS2-20661.
-- Add legendsetid column to programruleaction
ALTER TABLE programruleaction ADD COLUMN IF NOT EXISTS legendsetid int8;

-- Add foreign key to program rule action
ALTER TABLE programruleaction
    ADD CONSTRAINT fk_programruleaction_legendsetid
        FOREIGN KEY (legendsetid) REFERENCES maplegendset(maplegendsetid);