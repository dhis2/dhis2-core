-- rename programstageinstance to event
alter table if exists programinstance rename to enrollment;

do $$
begin
  if exists(select 1
    from information_schema.columns
    where table_schema != 'information_schema' and table_name='enrollment' and column_name='programinstanceid')
  then
alter table enrollment rename column programinstanceid to enrollmentid;
end if;
end $$;

-- rename programinstance_messageconversation to enrollment_messageconversation
alter table if exists programinstance_messageconversation rename to enrollment_messageconversation;

do $$
begin
  if exists(select 1
    from information_schema.columns
    where table_schema != 'information_schema' and table_name='enrollment_messageconversation' and column_name='programinstanceid')
  then
alter table enrollment_messageconversation rename column programinstanceid to enrollmentid;
end if;
end $$;

-- rename programinstancecomments to enrollmentcomments
alter table if exists programinstancecomments rename to enrollmentcomments;

do $$
begin
  if exists(select 1
    from information_schema.columns
    where table_schema != 'information_schema' and table_name='enrollmentcomments' and column_name='programinstanceid')
  then
alter table enrollmentcomments rename column programinstanceid to enrollmentid;
end if;
end $$;

-- rename programinstanceid in programmessage

do $$
begin
  if exists(select 1
    from information_schema.columns
    where table_schema != 'information_schema' and table_name='programmessage' and column_name='programinstanceid')
  then
alter table programmessage rename column programinstanceid to enrollmentid;
end if;
end $$;

-- rename programinstanceid in programnotificationinstance

do $$
begin
  if exists(select 1
    from information_schema.columns
    where table_schema != 'information_schema' and table_name='programnotificationinstance' and column_name='programinstanceid')
  then
alter table programnotificationinstance rename column programinstanceid to enrollmentid;
end if;
end $$;

-- rename programinstanceid in relationshipitem

do $$
begin
  if exists(select 1
    from information_schema.columns
    where table_schema != 'information_schema' and table_name='relationshipitem' and column_name='programinstanceid')
  then
alter table relationshipitem rename column programinstanceid to enrollmentid;
end if;
end $$;

-- rename programinstanceid in event

do $$
begin
  if exists(select 1
    from information_schema.columns
    where table_schema != 'information_schema' and table_name='event' and column_name='programinstanceid')
  then
alter table event rename column programinstanceid to enrollmentid;
end if;
end $$;