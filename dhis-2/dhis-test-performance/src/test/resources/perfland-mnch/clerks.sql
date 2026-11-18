-- Generates perfland-mnch/clerks.csv and perfland-mnch/children.csv of PerfLandMnchTrackerTest
-- from a perfland-mnch dump. Usage, in this directory:
--
--   psql --quiet --csv --file=clerks.sql
--
-- clerks.csv: dbgen's data clerks of district QP1613 the test logs in as, a virtual user each.
-- A clerk captures in her facility and searches in her district. The facilities differ by their
-- number of EIR children, as the query plans of the working lists differ between them: a clerk's
-- band is the range of her facility's. A range keeps away from the sizes at which a request
-- switches to another plan, so the statistics that ANALYZE samples before each run don't flip it:
-- of 11 facilities, each ANALYZEd 50 times in 2026-11, those near such a size flipped in up to 24%
-- of the rounds, the two of large in none. Clerks of facilities outside the ranges are left out.
-- The rows alternate the bands, so the first n clerks spread over them. The first of each band is
-- the one smoke measures, in EIR and in ANC: her facility keeps one plan of every banded request
-- in both, over 50 ANALYZEs each (2026-11). Median was Heguk until #26316, whose 413 ANC women
-- flipped ANC's first page in 9 of 50.
--
-- children.csv: the children each clerk works on, in order. A facility's clerks share its
-- children: each goes to one of them, picked uniformly with a fixed seed, the md5 of the child's
-- UID. A clerk's eq search sends the child's names, her like search their first 3 letters (as a
-- user types them), her EPI search the child's EPI, and she opens the child.
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
create index on children (given, family);
create index on children using gin (lower(given) gin_trgm_ops);
create index on children using gin (lower(family) gin_trgm_ops);
analyze children;

-- band values must match PerfLandMnchTrackerTest.Band.FACILITY
create temp table bands (band text, position int, min_children int, max_children int);
insert into bands values
  ('small', 1, 0, 200),
  ('median', 2, 275, 600),
  ('large', 3, 1080, 1250),
  ('largest', 4, 2500, 1000000);

-- the clerks smoke measures, the first of their band: small, median, large, largest
create temp table first_clerks (username text);
insert into first_clerks values
  ('mnch.qp1613-11-04.1'), ('mnch.qp1613-21-03.1'), ('mnch.qp1613-06-01.1'),
  ('mnch.qp1613-12-05.1');

-- dbgen's clerks mnch.<district>-<ward>-<facility>.<n> of the district's facilities. clerks and
-- clerk count all clerks of a facility, also those whose facility is in no band.
create temp table clerks as
select *, row_number() over (order by band_rank, band_position) as position
from (
  select c.*, b.band, b.position as band_position,
    row_number() over (
      partition by b.band
      order by (c.username in (select username from first_clerks)) desc, md5(c.username)
    ) as band_rank
  from (
    select u.username, ou.uid as orgunit, ou.organisationunitid, ou.path,
      (select count(*) from children ch where ch.owner = ou.organisationunitid) as eir,
      count(*) over (partition by ou.organisationunitid) as clerks,
      row_number() over (partition by ou.organisationunitid order by u.username) as clerk
    from userinfo u
    join usermembership m on m.userinfoid = u.userinfoid
    join organisationunit ou on ou.organisationunitid = m.organisationunitid
    where u.username ~ '^mnch\.qp1613-[0-9]{2}-[0-9]{2}\.[0-9]+$'
  ) c
  join bands b on c.eir between b.min_children and b.max_children
) x;

-- candidates: twice the children per clerk, as the 2.42 check below drops some. A facility's
-- children ordered by the md5 of their UID, which sorts the same in any collation, unlike the UID
-- itself, and dealt to its clerks in turn. district is the path of the facility's district, its
-- first 3 segments (root, region, district).
create temp table candidates as
select * from (
  select cl.username, cl.position, substring(cl.path from '^(?:/[^/]+){3}') as district, c.*,
    row_number() over (partition by cl.username order by c.k) as pick
  from clerks cl
  join (
    select ch.*, row_number() over (partition by ch.owner order by md5(ch.uid)) as k
    from children ch
  ) c on c.owner = cl.organisationunitid and (c.k - 1) % cl.clerks = cl.clerk - 1
) x
where pick <= 2 * :per_clerk;

-- the first per_clerk candidates whose eq and like searches pass on 2.42, over the matches in the
-- clerk's district, her search scope
create temp table picked as
select *, row_number() over (partition by username order by pick) as n
from candidates ca
where ca.owner in (
    select m.owner from children m
    where starts_with(m.owner_path, ca.district || '/')
      and m.given = ca.given and m.family = ca.family
    order by m.trackedentityid desc limit :page_size)
  and ca.owner in (
    select m.owner from children m
    where starts_with(m.owner_path, ca.district || '/')
      and lower(m.given) like '%' || lower(left(ca.given, 3)) || '%'
      and lower(m.family) like '%' || lower(left(ca.family, 3)) || '%'
    order by m.trackedentityid desc limit :page_size);

-- fail if a clerk has fewer than per_clerk children
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

\o clerks.csv
select username, orgunit as "orgUnit", band as "facilityBand"
from clerks
order by position;

\o children.csv
select username, given as "givenEq", family as "familyEq", lower(left(given, 3)) as "givenLike",
  lower(left(family, 3)) as "familyLike", epi, uid as "trackedEntity"
from picked
where n <= :per_clerk
order by position, n;
\o
