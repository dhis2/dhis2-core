-- Gives enrollments attribute option combos that the TrackerTest users can't read, to benchmark
-- the enrollment attribute option combo access check of the tracker exporters.
--
-- Creates an ATTRIBUTE category combo of two categories with :options_per_category category
-- options each, so :options_per_category^2 category option combos. No category option is
-- data-readable by anyone but its owner, so every one of these combos ends up in the list of
-- combos the exporters exclude. Then assigns these combos to :enrollment_percent percent of the
-- enrollments of :program.
--
-- The program's enrollment category combo stays unchanged, so new enrollments still get the
-- default combo. This only matters for the query cost being measured, not for metadata
-- consistency.
--
-- Variables: owner (username owning the category options), program (program UID),
-- options_per_category, enrollment_percent.

\set ON_ERROR_STOP on

begin;

select exists (select 1 from categorycombo where uid = 'PfEnrAocCcm') as already_populated \gset
\if :already_populated
  \echo 'Enrollment attribute option combos already populated, skipping'
  rollback;
  \quit
\endif

select uid as owner_uid from userinfo where username = :'owner' \gset

create temp table perf_sharing on commit drop as
select jsonb_build_object(
         'owner', :'owner_uid',
         'public', 'rw------',
         'external', false,
         'users', '{}'::jsonb,
         'userGroups', '{}'::jsonb) as sharing;

-- Category options, :options_per_category for each of the categories A and B
create temp table perf_option on commit drop as
select nextval('hibernate_sequence') as id, c.letter, i
from (values ('A'), ('B')) c(letter), generate_series(1, :options_per_category) i;

insert into categoryoption
  (categoryoptionid, uid, code, name, shortname, created, lastupdated, translations,
   attributevalues, sharing)
select o.id,
       'Pa' || o.letter || lpad(o.i::text, 8, '0'),
       'PERF_ENR_AOC_' || o.letter || o.i,
       'Perf enrollment AOC ' || o.letter || ' ' || o.i,
       'Perf enr AOC ' || o.letter || ' ' || o.i,
       now(), now(), '[]'::jsonb, '{}'::jsonb, s.sharing
from perf_option o, perf_sharing s;

-- Categories A and B
create temp table perf_category on commit drop as
select nextval('hibernate_sequence') as id, letter
from (values ('A'), ('B')) c(letter);

insert into category
  (categoryid, uid, code, name, shortname, created, lastupdated, datadimension,
   datadimensiontype, translations, attributevalues, sharing)
select c.id,
       'PfEnrAocCt' || c.letter,
       'PERF_ENR_AOC_' || c.letter,
       'Perf enrollment AOC ' || c.letter,
       'Perf enr AOC ' || c.letter,
       now(), now(), false, 'ATTRIBUTE', '[]'::jsonb, '{}'::jsonb, s.sharing
from perf_category c, perf_sharing s;

insert into categories_categoryoptions (categoryid, categoryoptionid, sort_order)
select c.id, o.id, o.i - 1
from perf_option o
join perf_category c using (letter);

-- Category combo of A and B
select nextval('hibernate_sequence') as combo_id \gset

insert into categorycombo
  (categorycomboid, uid, code, name, created, lastupdated, skiptotal, datadimensiontype,
   translations, sharing)
select :combo_id, 'PfEnrAocCcm', 'PERF_ENR_AOC', 'Perf enrollment AOC', now(), now(), false,
       'ATTRIBUTE', '[]'::jsonb, s.sharing
from perf_sharing s;

insert into categorycombos_categories (categorycomboid, categoryid, sort_order)
select :combo_id, c.id, case c.letter when 'A' then 0 else 1 end
from perf_category c;

-- Category option combos: every option of A with every option of B
create temp table perf_coc on commit drop as
select nextval('hibernate_sequence') as id,
       row_number() over (order by a.i, b.i) as rn,
       a.id as option_a,
       b.id as option_b
from perf_option a
cross join perf_option b
where a.letter = 'A' and b.letter = 'B';

insert into categoryoptioncombo
  (categoryoptioncomboid, uid, name, created, lastupdated, ignoreapproval, translations,
   attributevalues)
select id, 'Pc' || lpad(rn::text, 9, '0'), 'Perf enrollment AOC ' || rn, now(), now(), false,
       '[]'::jsonb, '{}'::jsonb
from perf_coc;

insert into categoryoptioncombos_categoryoptions (categoryoptioncomboid, categoryoptionid)
select id, option_a from perf_coc
union all
select id, option_b from perf_coc;

insert into categorycombos_optioncombos (categoryoptioncomboid, categorycomboid)
select id, :combo_id from perf_coc;

-- Assign the combos to a deterministic share of the program's enrollments
select count(*) as coc_count from perf_coc \gset

with target as (
  select en.enrollmentid, row_number() over (order by en.enrollmentid) as r
  from enrollment en
  join program p on p.programid = en.programid
  where p.uid = :'program'
    and (hashtext(en.uid) & 2147483647) % 100 < :enrollment_percent
)
update enrollment en
set attributeoptioncomboid = c.id
from target t
join perf_coc c on c.rn = ((t.r - 1) % :coc_count) + 1
where en.enrollmentid = t.enrollmentid;

commit;

select count(*) filter (where c.uid like 'Pc%') as perf_category_option_combos,
       (select count(*)
        from enrollment en
        join program p on p.programid = en.programid
        join categoryoptioncombo c on c.categoryoptioncomboid = en.attributeoptioncomboid
        where p.uid = :'program' and c.uid like 'Pc%') as restricted_enrollments,
       (select count(*)
        from enrollment en
        join program p on p.programid = en.programid
        where p.uid = :'program') as program_enrollments
from categoryoptioncombo c;
