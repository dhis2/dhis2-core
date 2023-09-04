-- rename trackedentitycomment to note
alter table if exists trackedentitycomment rename to note;

-- rename enrollmentcomments and eventcomments
alter table if exists enrollmentcomments rename to enrollmentnotes;
alter table if exists eventcomments rename to eventnotes;

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

-- rename trackedentitycommentid to noteid in enrollmentnotes
do $$
begin
  if exists(select 1
    from information_schema.columns
    where table_schema != 'information_schema' and table_name='enrollmentnotes' and column_name='trackedentitycommentid')
  then
alter table enrollmentnotes rename column trackedentitycommentid to noteid;
end if;
end $$;

-- rename trackedentitycommentid to noteid in eventnotes
do $$
begin
  if exists(select 1
    from information_schema.columns
    where table_schema != 'information_schema' and table_name='eventnotes' and column_name='trackedentitycommentid')
  then
alter table eventnotes rename column trackedentitycommentid to noteid;
end if;
end $$;
