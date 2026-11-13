-- Generates perfland-mnch/children.csv of PerfLandMnchTrackerTest from a perfland-mnch dump: 60
-- children per clerk, a row each. Usage:
--
--   psql --quiet --csv --file=children.sql > children.csv
--
-- A clerk searches and opens the children of her facility, picked uniformly with a fixed seed:
-- the md5 of the child's UID. Her eq search sends the child's names, her like search their first
-- 3 letters (as a user types them), her EPI search the child's EPI, and she opens the child. The
-- rows alternate the clerks, so each run of n iterations has the same children, in the same
-- order, n / 4 per facility band.
--
-- DHIS2 2.42 pages over the matches in the district (trackedentityid desc) and only then drops
-- what the clerk does not own. Her search returns nothing if none of the first page_size matches
-- is a child of her facility. A child is kept only if one of them is, for its eq and its like
-- search.

\set ON_ERROR_STOP on
\set program '''SSLpOM0r1U7'''
\set given '''sB1IHYu2xQT'''
\set family '''ENRjVGxVL6l'''
\set epi '''KSr2yTdu1AI'''
\set per_clerk 60
-- pageSize of the test's eq and like searches
\set page_size 5

-- band values must match PerfLandMnchTrackerTest.Band.FACILITY, its javadoc describes the clerks
create temp table clerks (username text, orgunit text, band text, position int);
insert into clerks values
  ('mnch.qp1613-11-04.1', 'spjwEIrPgzR', 'small', 1),
  ('mnch.qp1613-06-02.1', 'uGyaRDE33Zi', 'median', 2),
  ('mnch.qp1613-06-01.1', 'TYNeQQFirpB', 'large', 3),
  ('mnch.qp1613-12-05.1', 'ARiO87gt2ma', 'largest', 4);

-- EIR children with their names, EPI, owner and the owner's path. A child has one owner per
-- program, also after a transfer (break the glass adds a temporary owner, not another owner).
create temp table children as
select te.trackedentityid, te.uid, po.organisationunitid as owner, ou.path as owner_path,
  g.value as given, fa.value as family, e.value as epi
from trackedentityprogramowner po
join program p on p.programid = po.programid and p.uid = :program
join trackedentity te on te.trackedentityid = po.trackedentityid and not te.deleted
join organisationunit ou on ou.organisationunitid = po.organisationunitid
join trackedentityattributevalue g on g.trackedentityid = te.trackedentityid
  and g.trackedentityattributeid = (select trackedentityattributeid from trackedentityattribute where uid = :given)
join trackedentityattributevalue fa on fa.trackedentityid = te.trackedentityid
  and fa.trackedentityattributeid = (select trackedentityattributeid from trackedentityattribute where uid = :family)
join trackedentityattributevalue e on e.trackedentityid = te.trackedentityid
  and e.trackedentityattributeid = (select trackedentityattributeid from trackedentityattribute where uid = :epi)
where exists (
  select 1 from enrollment en
  where en.trackedentityid = te.trackedentityid and en.programid = po.programid and not en.deleted);

-- candidates: twice the children per clerk, as the 2.42 check below drops some. Ordered by the md5
-- of their UID, which sorts the same in any collation, unlike the UID itself. district is the path
-- of the facility's district, its first 3 segments (root, region, district).
create temp table candidates as
select * from (
  select cl.username, cl.orgunit, cl.band, cl.position, ou.organisationunitid,
    substring(ou.path from '^(?:/[^/]+){3}') as district, c.*,
    row_number() over (partition by cl.username order by md5(c.uid)) as pick
  from clerks cl
  join organisationunit ou on ou.uid = cl.orgunit
  join children c on c.owner = ou.organisationunitid
) x
where pick <= 2 * :per_clerk;

-- the first per_clerk candidates whose eq and like searches pass on 2.42, over the matches in the
-- clerk's district, her search scope
create temp table picked as
select *, row_number() over (partition by username order by pick) as n
from candidates ca
where ca.organisationunitid in (
    select m.owner from children m
    where starts_with(m.owner_path, ca.district || '/')
      and m.given = ca.given and m.family = ca.family
    order by m.trackedentityid desc limit :page_size)
  and ca.organisationunitid in (
    select m.owner from children m
    where starts_with(m.owner_path, ca.district || '/')
      and lower(m.given) like '%' || lower(left(ca.given, 3)) || '%'
      and lower(m.family) like '%' || lower(left(ca.family, 3)) || '%'
    order by m.trackedentityid desc limit :page_size);

-- fail if a clerk has fewer than per_clerk children, as the rows would no longer alternate the
-- bands
set children.per_clerk = :per_clerk;
do $$
declare
  short text;
begin
  select string_agg(cl.username, ', ') into short
  from clerks cl
  where (select count(*) from picked p where p.username = cl.username)
    < current_setting('children.per_clerk')::int;
  if short is not null then
    raise exception 'fewer than % children for %', current_setting('children.per_clerk'), short;
  end if;
end
$$;

select username, orgunit as "orgUnit", band as "facilityBand", given as "givenEq",
  family as "familyEq", lower(left(given, 3)) as "givenLike", lower(left(family, 3)) as "familyLike",
  epi, uid as "trackedEntity"
from picked
where n <= :per_clerk
order by n, position;
