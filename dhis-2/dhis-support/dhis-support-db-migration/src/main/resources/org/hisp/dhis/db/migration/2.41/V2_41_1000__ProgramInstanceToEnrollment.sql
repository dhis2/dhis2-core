-- rename programstageinstance to event
alter table if exists programinstance rename to enrollment;

DO $$
BEGIN
  IF EXISTS(SELECT 1
    FROM information_schema.columns
    WHERE table_schema != 'information_schema' and table_name='enrollment' and column_name='programinstanceid')
  THEN
ALTER TABLE enrollment RENAME COLUMN programinstanceid TO enrollmentid;
END IF;
END $$;