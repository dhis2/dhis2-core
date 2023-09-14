-- rename trackedentitycomment to note
alter table if exists trackedentitycomment rename to note;

-- rename enrollmentcomments and eventcomments
alter table if exists enrollmentcomments rename to enrollment_notes;
alter table if exists eventcomments rename to event_notes;

-- rename trackedentitycommentid to noteid in note
do $$
begin
  if exists(select 1
    from information_schema.columns
    where table_schema != 'information_schema' and table_name='note' and column_name='trackedentitycommentid')
  then
alter table note rename column trackedentitycommentid to noteid;
end if;
end $$;

-- rename commenttext to notetext in note
do $$
begin
  if exists(select 1
    from information_schema.columns
    where table_schema != 'information_schema' and table_name='note' and column_name='commenttext')
  then
alter table note rename column commenttext to notetext;
end if;
end $$;

-- rename trackedentitycommentid to noteid in enrollment_notes
do $$
begin
  if exists(select 1
    from information_schema.columns
    where table_schema != 'information_schema' and table_name='enrollment_notes' and column_name='trackedentitycommentid')
  then
alter table enrollment_notes rename column trackedentitycommentid to noteid;
end if;
end $$;

-- rename trackedentitycommentid to noteid in event_notes
do $$
begin
  if exists(select 1
    from information_schema.columns
    where table_schema != 'information_schema' and table_name='event_notes' and column_name='trackedentitycommentid')
  then
alter table event_notes rename column trackedentitycommentid to noteid;
end if;
end $$;
