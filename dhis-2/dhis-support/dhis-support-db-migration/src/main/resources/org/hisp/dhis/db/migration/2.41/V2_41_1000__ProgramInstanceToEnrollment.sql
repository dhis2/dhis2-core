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

-- rename programinstance_messageconversation to enrollment_messageconversation
alter table if exists programinstance_messageconversation rename to enrollment_messageconversation;

DO $$
BEGIN
  IF EXISTS(SELECT 1
    FROM information_schema.columns
    WHERE table_schema != 'information_schema' and table_name='enrollment_messageconversation' and column_name='programinstanceid')
  THEN
ALTER TABLE enrollment_messageconversation RENAME COLUMN programinstanceid TO enrollmentid;
END IF;
END $$;

-- rename programinstancecomments to enrollmentcomments
alter table if exists programinstancecomments rename to enrollmentcomments;

DO $$
BEGIN
  IF EXISTS(SELECT 1
    FROM information_schema.columns
    WHERE table_schema != 'information_schema' and table_name='enrollmentcomments' and column_name='programinstanceid')
  THEN
ALTER TABLE enrollmentcomments RENAME COLUMN programinstanceid TO enrollmentid;
END IF;
END $$;

-- rename programinstanceid in programmessage

DO $$
BEGIN
  IF EXISTS(SELECT 1
    FROM information_schema.columns
    WHERE table_schema != 'information_schema' and table_name='programmessage' and column_name='programinstanceid')
  THEN
ALTER TABLE programmessage RENAME COLUMN programinstanceid TO enrollmentid;
END IF;
END $$;

-- rename programinstanceid in programnotificationinstance

DO $$
BEGIN
  IF EXISTS(SELECT 1
    FROM information_schema.columns
    WHERE table_schema != 'information_schema' and table_name='programnotificationinstance' and column_name='programinstanceid')
  THEN
ALTER TABLE programnotificationinstance RENAME COLUMN programinstanceid TO enrollmentid;
END IF;
END $$;

-- rename programinstanceid in relationshipitem

DO $$
BEGIN
  IF EXISTS(SELECT 1
    FROM information_schema.columns
    WHERE table_schema != 'information_schema' and table_name='relationshipitem' and column_name='programinstanceid')
  THEN
ALTER TABLE relationshipitem RENAME COLUMN programinstanceid TO enrollmentid;
END IF;
END $$;

-- rename programinstanceid in event

DO $$
BEGIN
  IF EXISTS(SELECT 1
    FROM information_schema.columns
    WHERE table_schema != 'information_schema' and table_name='event' and column_name='programinstanceid')
  THEN
ALTER TABLE event RENAME COLUMN programinstanceid TO enrollmentid;
END IF;
END $$;