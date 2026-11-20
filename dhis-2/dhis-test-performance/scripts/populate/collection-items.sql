-- Populates an org unit group with many members, so the collection items endpoints
-- (POST/DELETE /api/organisationUnitGroups/{uid}/organisationUnits) work on a large collection.
--
-- Run through scripts/populate-collection-items.sh, which creates the group and the parent org
-- unit and sets the variables below.
--
-- Author: Morten Svanæs <msvanaes@dhis2.org>
--
-- Variables:
--   group_uid        UID of the (existing) org unit group that gets the members
--   parent_uid       UID of the (existing) org unit that is the parent of the populated org units
--   members          number of populated org units in the group, UIDs PfCm0000001, PfCm0000002, ...
--   non_members      number of populated org units not in the group, UIDs PfCa0000001, ...; the
--                    simulation adds these to the group
--
-- Populated org units have a UID matching ^PfC[ma][0-9]{7}$. A run first deletes the ones of a
-- previous run and empties the group, so it can be repeated with different parameters.

\set ON_ERROR_STOP on

begin;

select orgunitgroupid as group_id
from orgunitgroup
where uid = :'group_uid' \gset

select organisationunitid as parent_id, path as parent_path, hierarchylevel as parent_level
from organisationunit
where uid = :'parent_uid' \gset

delete from orgunitgroupmembers
where orgunitgroupid = :group_id
   or organisationunitid in (select organisationunitid from organisationunit where uid ~ '^PfC[ma][0-9]{7}$');

delete from organisationunit
where uid ~ '^PfC[ma][0-9]{7}$';

\echo Populating group :group_uid with :members members and :non_members non members

create temporary table populate_org_unit on commit drop as
select 'PfCm' || lpad(n::text, 7, '0') as uid, true as member
from generate_series(1, :members) n
union all
select 'PfCa' || lpad(n::text, 7, '0') as uid, false as member
from generate_series(1, :non_members) n;

-- org units use the "native" id generator, which is hibernate_sequence on PostgreSQL
alter table populate_org_unit add column organisationunitid bigint;
update populate_org_unit set organisationunitid = nextval('hibernate_sequence');

-- Since 2.44 path and hierarchylevel are generated from patharray, which a trigger sets from the
-- parent; before, they are plain columns
select exists (
  select 1 from information_schema.columns
  where table_name = 'organisationunit' and column_name = 'patharray') as has_patharray \gset

\if :has_patharray
insert into organisationunit (
  organisationunitid, uid, name, shortname, parentid, openingdate, created, lastupdated)
select
  organisationunitid, uid, 'Perf collection item ' || uid, 'Perf ' || uid, :parent_id,
  date '2020-01-01', now(), now()
from populate_org_unit;
\else
insert into organisationunit (
  organisationunitid, uid, name, shortname, parentid, path, hierarchylevel, openingdate,
  created, lastupdated)
select
  organisationunitid, uid, 'Perf collection item ' || uid, 'Perf ' || uid, :parent_id,
  :'parent_path' || '/' || uid, :parent_level + 1, date '2020-01-01', now(), now()
from populate_org_unit;
\endif

insert into orgunitgroupmembers (orgunitgroupid, organisationunitid)
select :group_id, organisationunitid
from populate_org_unit
where member;

commit;

analyze organisationunit;
analyze orgunitgroupmembers;

select count(*) as group_members
from orgunitgroupmembers
where orgunitgroupid = :group_id \gset
\echo Group :group_uid has :group_members members
