-- Assigns attribute option combos (AOCs) to enrollments, so the tracked entity export has to check
-- the user's data read access to the category options of every enrollment it looks at.
--
-- Run through scripts/populate-enrollment-attribute-option-combo.sh, which creates the category
-- combo and the user, and sets the variables below.
--
-- Variables:
--   program_uid          UID of the program whose enrollments all get an AOC of the combo
--   category_combo_uid   UID of the (existing) attribute category combo, with its AOCs generated
--   username             user added to every user group, so the sharing check compares many groups
--
-- Enrollments get the combo's AOCs round robin in id order, so every AOC holds the same share of
-- them.
-- Every category option of the combo gets shared with every user group without data access, so the
-- sharing check reads large sharing objects but grants nothing beyond the public access.

\set ON_ERROR_STOP on

begin;

select programid as program_id
from program
where uid = :'program_uid' \gset

select categorycomboid as category_combo_id
from categorycombo
where uid = :'category_combo_uid' \gset

select userinfoid as user_id
from userinfo
where username = :'username' \gset

create temporary table populate_aoc on commit drop as
select
  cco.categoryoptioncomboid,
  row_number() over (order by cco.categoryoptioncomboid) - 1 as position
from categorycombos_optioncombos cco
where cco.categorycomboid = :category_combo_id;

select count(*) as aocs from populate_aoc \gset
\echo Assigning :aocs AOCs to the enrollments of program :program_uid

-- number the enrollments, as their ids have gaps that would skew a modulo of the id
update enrollment e
set attributeoptioncomboid = aoc.categoryoptioncomboid
from (
    select enrollmentid, row_number() over (order by enrollmentid) - 1 as position
    from enrollment
    where programid = :program_id
  ) numbered
  join populate_aoc aoc on aoc.position = numbered.position % :aocs
where e.enrollmentid = numbered.enrollmentid;

update categoryoption co
set sharing = jsonb_set(
  co.sharing,
  '{userGroups}',
  (select jsonb_object_agg(ug.uid, jsonb_build_object('id', ug.uid, 'access', 'r-------'))
   from usergroup ug))
from categories_categoryoptions cco
join categorycombos_categories ccc on ccc.categoryid = cco.categoryid
where cco.categoryoptionid = co.categoryoptionid
  and ccc.categorycomboid = :category_combo_id;

insert into usergroupmembers (usergroupid, userid)
select ug.usergroupid, :user_id
from usergroup ug
on conflict do nothing;

commit;

analyze enrollment;
