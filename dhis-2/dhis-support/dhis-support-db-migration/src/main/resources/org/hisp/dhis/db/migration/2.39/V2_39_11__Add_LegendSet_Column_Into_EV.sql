-- This script relates to the ticket https://jira.dhis2.org/browse/DHIS2-13264
-- It adds new columns into the Event Visualization table.

ALTER TABLE eventvisualization ADD COLUMN IF NOT EXISTS legendsetid int8 null;

DO $$
BEGIN
  BEGIN
    ALTER TABLE eventvisualization ADD CONSTRAINT fk_evisualization_legendsetid FOREIGN KEY (legendsetid) REFERENCES maplegendset(maplegendsetid);
  EXCEPTION
  	WHEN OTHERS THEN
    	RAISE NOTICE 'Table constraint % already exists', 'fk_evisualization_legendsetid';
  END;
END $$;

ALTER TABLE eventvisualization ADD COLUMN IF NOT EXISTS legenddisplaystrategy character varying(40);

ALTER TABLE eventvisualization ADD COLUMN IF NOT EXISTS legenddisplaystyle character varying(40);

ALTER TABLE eventvisualization ADD COLUMN IF NOT EXISTS legendshowkey boolean;
