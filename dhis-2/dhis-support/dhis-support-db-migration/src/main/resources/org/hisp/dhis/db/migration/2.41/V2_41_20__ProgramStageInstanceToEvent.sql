-- rename programstageinstance to event
alter table if exists programstageinstance rename to event;

do $$
begin
  if exists(select 1
    from information_schema.columns
    where table_schema != 'information_schema' and table_name='event' and column_name='programstageinstanceid')
  then
alter table event rename column programstageinstanceid to eventid;
end if;
end $$;

-- rename programstageinstancefilter to eventfilter
alter table if exists programstageinstancefilter rename to eventfilter;

do $$
begin
  if exists(select 1
    from information_schema.columns
    where table_schema != 'information_schema' and table_name='eventfilter' and column_name='programstageinstancefilterid')
  then
alter table eventfilter rename column programstageinstancefilterid to eventfilterid;
end if;
end $$;


-- rename programstageinstanceid in relationshipitem
do $$
begin
  if exists(select 1
    from information_schema.columns
    where table_schema != 'information_schema' and table_name='relationshipitem' and column_name='programstageinstanceid')
  then
alter table relationshipitem rename column programstageinstanceid to eventid;
end if;
end $$;

-- rename programstageinstanceid in trackedentitydatavalueaudit
do $$
begin
  if exists(select 1
    from information_schema.columns
    where table_schema != 'information_schema' and table_name='trackedentitydatavalueaudit' and column_name='programstageinstanceid')
  then
alter table trackedentitydatavalueaudit rename column programstageinstanceid to eventid;
end if;
end $$;

-- rename programstageinstanceid in programmessage
do $$
begin
  if exists(select 1
    from information_schema.columns
    where table_schema != 'information_schema' and table_name='programmessage' and column_name='programstageinstanceid')
  then
alter table programmessage rename column programstageinstanceid to eventid;
end if;
end $$;

-- rename programstageinstanceid in programnotificationinstance
do $$
begin
  if exists(select 1
    from information_schema.columns
    where table_schema != 'information_schema' and table_name='programnotificationinstance' and column_name='programstageinstanceid')
  then
alter table programnotificationinstance rename column programstageinstanceid to eventid;
end if;
end $$;

-- rename programstageinstancecomments
alter table if exists programstageinstancecomments rename to eventcomments;

do $$
begin
  if exists(select 1
    from information_schema.columns
    where table_schema != 'information_schema' and table_name='eventcomments' and column_name='programstageinstanceid')
  then
alter table eventcomments rename column programstageinstanceid to eventid;
end if;
end $$;

-- rename programstageinstance_messageconversation
alter table if exists programstageinstance_messageconversation rename to event_messageconversation;

do $$
begin
  if exists(select 1
    from information_schema.columns
    where table_schema != 'information_schema' and table_name='event_messageconversation' and column_name='programstageinstanceid')
  then
alter table event_messageconversation rename column programstageinstanceid to eventid;
end if;
end $$;