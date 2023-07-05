-- rename programstageinstance to event
alter table if exists programstageinstance rename to event;

DO $$
BEGIN
  IF EXISTS(SELECT 1
    FROM information_schema.columns
    WHERE table_schema != 'information_schema' and table_name='event' and column_name='programstageinstanceid')
  THEN
ALTER TABLE event RENAME COLUMN programstageinstanceid TO eventid;
END IF;
END $$;

-- rename programstageinstancefilter to eventfilter
alter table if exists programstageinstancefilter rename to eventfilter;

DO $$
BEGIN
  IF EXISTS(SELECT 1
    FROM information_schema.columns
    WHERE table_schema != 'information_schema' and table_name='eventfilter' and column_name='programstageinstancefilterid')
  THEN
ALTER TABLE eventfilter RENAME COLUMN programstageinstancefilterid TO eventfilterid;
END IF;
END $$;


-- rename programstageinstanceid in relationshipitem
DO $$
BEGIN
  IF EXISTS(SELECT 1
    FROM information_schema.columns
    WHERE table_schema != 'information_schema' and table_name='relationshipitem' and column_name='programstageinstanceid')
  THEN
ALTER TABLE relationshipitem RENAME COLUMN programstageinstanceid TO eventid;
END IF;
END $$;

-- rename programstageinstanceid in trackedentitydatavalueaudit
DO $$
BEGIN
  IF EXISTS(SELECT 1
    FROM information_schema.columns
    WHERE table_schema != 'information_schema' and table_name='trackedentitydatavalueaudit' and column_name='programstageinstanceid')
  THEN
ALTER TABLE trackedentitydatavalueaudit RENAME COLUMN programstageinstanceid TO eventid;
END IF;
END $$;

-- rename programstageinstanceid in programmessage
DO $$
BEGIN
  IF EXISTS(SELECT 1
    FROM information_schema.columns
    WHERE table_schema != 'information_schema' and table_name='programmessage' and column_name='programstageinstanceid')
  THEN
ALTER TABLE programmessage RENAME COLUMN programstageinstanceid TO eventid;
END IF;
END $$;

-- rename programstageinstanceid in programnotificationinstance
DO $$
BEGIN
  IF EXISTS(SELECT 1
    FROM information_schema.columns
    WHERE table_schema != 'information_schema' and table_name='programnotificationinstance' and column_name='programstageinstanceid')
  THEN
ALTER TABLE programnotificationinstance RENAME COLUMN programstageinstanceid TO eventid;
END IF;
END $$;

-- rename programstageinstancecomments
alter table if exists programstageinstancecomments rename to eventcomments;

DO $$
BEGIN
  IF EXISTS(SELECT 1
    FROM information_schema.columns
    WHERE table_schema != 'information_schema' and table_name='eventcomments' and column_name='programstageinstanceid')
  THEN
ALTER TABLE eventcomments RENAME COLUMN programstageinstanceid TO eventid;
END IF;
END $$;

-- rename programstageinstance_messageconversation
alter table if exists programstageinstance_messageconversation rename to event_messageconversation;

DO $$
BEGIN
  IF EXISTS(SELECT 1
    FROM information_schema.columns
    WHERE table_schema != 'information_schema' and table_name='event_messageconversation' and column_name='programstageinstanceid')
  THEN
ALTER TABLE event_messageconversation RENAME COLUMN programstageinstanceid TO eventid;
END IF;
END $$;