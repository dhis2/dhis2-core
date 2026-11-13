-- Generates perfland-mnch/cases.csv of PerfLandMnchTrackerTest from a perfland-mnch dump: 60
-- children per clerk, each a case. Usage:
--
--   psql --quiet --csv --file=cases.sql > cases.csv
--
-- A clerk searches and opens the children of her facility, so a case is one of them, picked
-- uniformly with a fixed seed: the md5 of the child's UID. Her eq search sends the child's names,
-- her like search their first 3 letters (as a user types them), her EPI search the child's EPI,
-- and she opens the child. The rows alternate the clerks, so each run of n iterations has the
-- same cases, in the same order, n / 4 per facility band.
--
-- DHIS2 2.42 pages over the matches in the district and drops what the clerk does not own after
-- paging (trackedentityid desc, pageSize=5). A case is kept only if the child, or another child
-- of the clerk, is among the first 5 matches of its eq and its like search in the district.

\set program '''SSLpOM0r1U7'''
\set given '''sB1IHYu2xQT'''
\set family '''ENRjVGxVL6l'''
\set epi '''KSr2yTdu1AI'''
\set per_clerk 60

create temp table clerks (username text, orgunit text, band text, position int);
insert into clerks values
  ('mnch.qp1613-11-04.1', 'spjwEIrPgzR', 'small', 1),
  ('mnch.qp1613-06-02.1', 'uGyaRDE33Zi', 'median', 2),
  ('mnch.qp1613-06-01.1', 'TYNeQQFirpB', 'large', 3),
  ('mnch.qp1613-12-05.1', 'ARiO87gt2ma', 'largest', 4);

-- EIR children of the district with their names, EPI and owner
create temp table children as
select te.trackedentityid, te.uid, po.organisationunitid as owner,
  g.value as given, fa.value as family, e.value as epi
from enrollment en
join program p on p.programid = en.programid and p.uid = :program
join trackedentity te on te.trackedentityid = en.trackedentityid
join trackedentityprogramowner po on po.trackedentityid = te.trackedentityid
  and po.programid = en.programid
join trackedentityattributevalue g on g.trackedentityid = te.trackedentityid
  and g.trackedentityattributeid = (select trackedentityattributeid from trackedentityattribute where uid = :given)
join trackedentityattributevalue fa on fa.trackedentityid = te.trackedentityid
  and fa.trackedentityattributeid = (select trackedentityattributeid from trackedentityattribute where uid = :family)
join trackedentityattributevalue e on e.trackedentityid = te.trackedentityid
  and e.trackedentityattributeid = (select trackedentityattributeid from trackedentityattribute where uid = :epi);

-- candidates: twice the cases per clerk, in the order of the md5 of their UID
create temp table candidates as
select * from (
  select cl.username, cl.orgunit, cl.band, cl.position, ou.organisationunitid, c.*,
    row_number() over (partition by cl.username order by md5(c.uid)) as pick
  from clerks cl
  join organisationunit ou on ou.uid = cl.orgunit
  join children c on c.owner = ou.organisationunitid
) x
where pick <= 2 * :per_clerk;

-- the first per_clerk candidates whose eq and like searches pass on 2.42
create temp table cases as
select *, row_number() over (partition by username order by pick) as n
from candidates ca
where ca.organisationunitid in (
    select m.owner from children m
    where m.given = ca.given and m.family = ca.family
    order by m.trackedentityid desc limit 5)
  and ca.organisationunitid in (
    select m.owner from children m
    where lower(m.given) like '%' || lower(left(ca.given, 3)) || '%'
      and lower(m.family) like '%' || lower(left(ca.family, 3)) || '%'
    order by m.trackedentityid desc limit 5);

select username, orgunit as "orgUnit", band as "facilityBand", given as "givenEq",
  family as "familyEq", lower(left(given, 3)) as "givenLike", lower(left(family, 3)) as "familyLike",
  epi, uid as "trackedEntity"
from cases
where n <= :per_clerk
order by n, position;
