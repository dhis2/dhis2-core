create sequence if not exists trackereventchangelog_sequence;
create sequence if not exists singleeventchangelog_sequence;

select setval('trackereventchangelog_sequence', coalesce(max(eventchangelogid), 1)) from trackereventchangelog;
select setval('singleeventchangelog_sequence', coalesce(max(eventchangelogid),1)) from singleeventchangelog;

alter table trackereventchangelog alter column eventchangelogid drop default;
alter table singleeventchangelog alter column eventchangelogid drop default;

alter table trackereventchangelog alter column eventchangelogid set default nextval('trackereventchangelog_sequence');
alter table singleeventchangelog alter column eventchangelogid set default nextval('singleeventchangelog_sequence');

drop sequence if exists eventchangelog_sequence;
