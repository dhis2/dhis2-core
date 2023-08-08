-- rename trackedentityinstance to trackedentity
alter table if exists trackedentityinstance rename to trackedentity;

do $$
begin
  if exists(select 1
    from information_schema.columns
    where table_schema != 'information_schema' and table_name='trackedentity' and column_name='trackedentityinstanceid')
  then
alter table trackedentity rename column trackedentityinstanceid to trackedentityid;
end if;
end $$;

-- rename trackedentityinstanceaudit

alter table if exists trackedentityinstanceaudit rename to trackedentityaudit;

do $$
begin
  if exists(select 1
    from information_schema.columns
    where table_schema != 'information_schema' and table_name='trackedentityaudit' and column_name='trackedentityinstance')
  then
alter table trackedentityaudit rename column trackedentityinstance to trackedentity;
end if;
end $$;

do $$
begin
  if exists(select 1
    from information_schema.columns
    where table_schema != 'information_schema' and table_name='trackedentityaudit' and column_name='trackedentityinstanceauditid')
  then
alter table trackedentityaudit rename column trackedentityinstanceauditid to trackedentityauditid;
end if;
end $$;

-- rename trackedentityinstancefilter

alter table if exists trackedentityinstancefilter rename to trackedentityfilter;

do $$
begin
  if exists(select 1
    from information_schema.columns
    where table_schema != 'information_schema' and table_name='trackedentityfilter' and column_name='trackedentityinstancefilterid')
  then
alter table trackedentityfilter rename column trackedentityinstancefilterid to trackedentityfilterid;
end if;
end $$;

-- rename trackedentityinstanceid in enrollment

do $$
begin
  if exists(select 1
    from information_schema.columns
    where table_schema != 'information_schema' and table_name='enrollment' and column_name='trackedentityinstanceid')
  then
alter table enrollment rename column trackedentityinstanceid to trackedentityid;
end if;
end $$;

-- rename trackedentityinstanceid in trackedentityattributevalue

do $$
begin
  if exists(select 1
    from information_schema.columns
    where table_schema != 'information_schema' and table_name='trackedentityattributevalue' and column_name='trackedentityinstanceid')
  then
alter table trackedentityattributevalue rename column trackedentityinstanceid to trackedentityid;
end if;
end $$;

-- rename trackedentityinstanceid in trackedentityattributevalueaudit

do $$
begin
  if exists(select 1
    from information_schema.columns
    where table_schema != 'information_schema' and table_name='trackedentityattributevalueaudit' and column_name='trackedentityinstanceid')
  then
alter table trackedentityattributevalueaudit rename column trackedentityinstanceid to trackedentityid;
end if;
end $$;

-- rename trackedentityinstanceid in programmessage

do $$
begin
  if exists(select 1
    from information_schema.columns
    where table_schema != 'information_schema' and table_name='programmessage' and column_name='trackedentityinstanceid')
  then
alter table programmessage rename column trackedentityinstanceid to trackedentityid;
end if;
end $$;

-- rename trackedentityinstanceid in relationshipitem

do $$
begin
  if exists(select 1
    from information_schema.columns
    where table_schema != 'information_schema' and table_name='relationshipitem' and column_name='trackedentityinstanceid')
  then
alter table relationshipitem rename column trackedentityinstanceid to trackedentityid;
end if;
end $$;

-- rename trackedentityinstanceid in trackedentityprogramowner

do $$
begin
  if exists(select 1
    from information_schema.columns
    where table_schema != 'information_schema' and table_name='trackedentityprogramowner' and column_name='trackedentityinstanceid')
  then
alter table trackedentityprogramowner rename column trackedentityinstanceid to trackedentityid;
end if;
end $$;

-- rename trackedentityinstanceid in programtempownershipaudit

do $$
begin
  if exists(select 1
    from information_schema.columns
    where table_schema != 'information_schema' and table_name='programtempownershipaudit' and column_name='trackedentityinstanceid')
  then
alter table programtempownershipaudit rename column trackedentityinstanceid to trackedentityid;
end if;
end $$;

-- rename trackedentityinstanceid in programtempowner

do $$
begin
  if exists(select 1
    from information_schema.columns
    where table_schema != 'information_schema' and table_name='programtempowner' and column_name='trackedentityinstanceid')
  then
alter table programtempowner rename column trackedentityinstanceid to trackedentityid;
end if;
end $$;

-- rename trackedentityinstanceid in programownershiphistory

do $$
begin
  if exists(select 1
    from information_schema.columns
    where table_schema != 'information_schema' and table_name='programownershiphistory' and column_name='trackedentityinstanceid')
  then
alter table programownershiphistory rename column trackedentityinstanceid to trackedentityid;
end if;
end $$;


