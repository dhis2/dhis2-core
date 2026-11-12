-- TrackedEntityChangeLog, TrackerEventChangeLog, SingleEventChangeLog and PotentialDuplicate
-- declared an unnamed @SequenceGenerator, which Hibernate 5 ignores. Their ids were therefore
-- drawn from hibernate_sequence instead of the dedicated sequence, while the JDBC changelog
-- inserts (ChangeLogAccumulator) use the column default, i.e. the dedicated sequence. The two
-- sources overlap and cause duplicate key violations on the primary key.
--
-- The entities now reference their dedicated sequence. Move each sequence past any id already
-- taken from hibernate_sequence so the switch cannot collide with an existing row.
select setval('trackedentitychangelog_sequence', greatest(
    (select last_value from trackedentitychangelog_sequence),
    coalesce((select max(trackedentitychangelogid) from trackedentitychangelog), 1)));

select setval('trackereventchangelog_sequence', greatest(
    (select last_value from trackereventchangelog_sequence),
    coalesce((select max(eventchangelogid) from trackereventchangelog), 1)));

select setval('singleeventchangelog_sequence', greatest(
    (select last_value from singleeventchangelog_sequence),
    coalesce((select max(eventchangelogid) from singleeventchangelog), 1)));

select setval('potentialduplicatesequence', greatest(
    (select last_value from potentialduplicatesequence),
    coalesce((select max(potentialduplicateid) from potentialduplicate), 1)));
