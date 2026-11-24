-- Generates perfland-mnch/women.csv of PerfLandMnchTrackerTest's ANC scenario from a perfland-mnch
-- dump and clerks.csv. Usage, in this directory, after clerks.sql:
--
--   psql --quiet --csv --file=women.sql
--
-- women.csv: up to 60 women per clerk of clerks.csv, of her facility, each with an ACTIVE
-- enrollment that has a Quick Check event, dealt to the facility's clerks in turn by the md5 of
-- the woman's UID. Clerks of small facilities get fewer, down to 27; the test cycles through them.
-- Every dbgen clerk is in both programs' data capture groups. Her ANC ID (ANC.A4, distinct in the
-- dump though not a unique attribute) is what the clerk searches by, the whole ID with like (as
-- Capture sends it) and with eq.

\set ON_ERROR_STOP on
\set program '''WSGAb5XwJ3Y'''
\set ancid '''PpEGiQurAll'''
\set quickcheck '''XznDErihed9'''
\set per_clerk 60

create temp table csv_clerks (username text, orgunit text, band text, position serial);
\copy csv_clerks (username, orgunit, band) from program 'tail -n +2 clerks.csv' csv

create temp table women as
select te.trackedentityid, te.uid, po.organisationunitid as owner, a.value as ancid
from trackedentityprogramowner po
join program p on p.programid = po.programid and p.uid = :program
join trackedentity te on te.trackedentityid = po.trackedentityid and not te.deleted
join trackedentityattributevalue a on a.trackedentityid = te.trackedentityid
  and a.trackedentityattributeid = (select trackedentityattributeid from trackedentityattribute where uid = :ancid)
where exists (
  select 1 from enrollment en
  join trackerevent ev on ev.enrollmentid = en.enrollmentid and not ev.deleted
  join programstage ps on ps.programstageid = ev.programstageid and ps.uid = :quickcheck
  where en.trackedentityid = te.trackedentityid and en.programid = po.programid
    and en.status = 'ACTIVE' and not en.deleted);

create temp table clerks as
select c.username, c.position, ou.organisationunitid,
  count(*) over (partition by ou.organisationunitid) as clerks,
  row_number() over (partition by ou.organisationunitid order by c.username) as clerk
from csv_clerks c
join organisationunit ou on ou.uid = c.orgunit;

create temp table picked as
select * from (
  select cl.username, cl.position, w.*,
    row_number() over (partition by cl.username order by w.k) as n
  from clerks cl
  join (
    select w.*, row_number() over (partition by w.owner order by md5(w.uid)) as k from women w
  ) w on w.owner = cl.organisationunitid and (w.k - 1) % cl.clerks = cl.clerk - 1
) x
where n <= :per_clerk;

-- fail if a clerk has no women
do $$
declare
  none text;
begin
  select string_agg(cl.username, ', ') into none
  from clerks cl
  where not exists (select 1 from picked p where p.username = cl.username);
  if none is not null then
    raise exception 'no women for %', none;
  end if;
end
$$;

\o women.csv
select username, ancid as "ancId", uid as "trackedEntity"
from picked
order by position, n;
\o
