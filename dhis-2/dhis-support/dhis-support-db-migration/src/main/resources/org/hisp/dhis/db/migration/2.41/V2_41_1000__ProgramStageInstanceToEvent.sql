alter table if exists programstageinstance rename to event;

DO $$
BEGIN
  IF EXISTS(SELECT *
    FROM information_schema.columns
    WHERE table_name='event' and column_name='programstageinstanceid')
  THEN
ALTER TABLE event RENAME COLUMN "programstageinstanceid" TO "eventid";
END IF;
END $$;