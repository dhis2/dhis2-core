-- Populates tracked entities holding values of a unique tracked entity attribute, so tracker
-- imports sending values of that attribute find them already stored in other org units.
--
-- Run through scripts/populate-unique-attribute.sh, which creates the attribute and sets the
-- variables below.
--
-- Variables:
--   attribute_uid         UID of the (existing) unique tracked entity attribute
--   tracked_entity_type   UID of the tracked entity type of the populated tracked entities
--   excluded_org_unit     UID of the org unit the import creates tracked entities in; it gets no
--                         populated values, so imported values never collide within it
--   org_units             number of org units (lowest hierarchy level, ordered by id) that get
--                         the values
--   values_per_org_unit   every one of these org units gets one tracked entity per value, with
--                         the values '1'..'<values_per_org_unit>'
--
-- Populated tracked entities have a UID matching ^Pf[0-9]{9}$. A run first deletes the ones of a
-- previous run, so it can be repeated with different parameters.

\set ON_ERROR_STOP on

begin;

select trackedentityattributeid as attribute_id
from trackedentityattribute
where uid = :'attribute_uid' \gset

select trackedentitytypeid as tracked_entity_type_id
from trackedentitytype
where uid = :'tracked_entity_type' \gset

delete from trackedentityattributevalue v
using trackedentity te
where v.trackedentityid = te.trackedentityid
  and te.uid ~ '^Pf[0-9]{9}$';

delete from trackedentity
where uid ~ '^Pf[0-9]{9}$';

create temporary table populate_org_unit on commit drop as
select organisationunitid, row_number() over (order by organisationunitid) as position
from organisationunit
where uid <> :'excluded_org_unit'
  and hierarchylevel = (select max(hierarchylevel) from organisationunit)
order by organisationunitid
limit :org_units;

select count(*) as populated_org_units from populate_org_unit \gset
\echo Populating :populated_org_units org units (requested :org_units) with :values_per_org_unit values each

select coalesce(max(trackedentityid), 0) as max_tracked_entity_id from trackedentity \gset

create temporary table populate_value on commit drop as
select
  :max_tracked_entity_id + (ou.position - 1) * :values_per_org_unit + v as trackedentityid,
  (ou.position - 1) * :values_per_org_unit + v as n,
  ou.organisationunitid,
  v::text as value
from populate_org_unit ou
cross join generate_series(1, :values_per_org_unit) v;

insert into trackedentity (
  trackedentityid, uid, created, lastupdated, createdatclient, lastupdatedatclient,
  inactive, deleted, potentialduplicate, organisationunitid, trackedentitytypeid)
select
  trackedentityid, 'Pf' || lpad(n::text, 9, '0'), now(), now(), now(), now(),
  false, false, false, organisationunitid, :tracked_entity_type_id
from populate_value;

insert into trackedentityattributevalue (
  trackedentityid, trackedentityattributeid, value, created, lastupdated)
select trackedentityid, :attribute_id, value, now(), now()
from populate_value;

-- keep the id sequence ahead of the inserted ids (it was renamed in 2.44)
select setval(c.oid::regclass, (select max(trackedentityid) from trackedentity)) as last_id
from pg_class c
where c.relkind = 'S'
  and c.relname in ('trackedentity_sequence', 'trackedentityinstance_sequence') \gset

commit;

analyze trackedentity;
analyze trackedentityattributevalue;
