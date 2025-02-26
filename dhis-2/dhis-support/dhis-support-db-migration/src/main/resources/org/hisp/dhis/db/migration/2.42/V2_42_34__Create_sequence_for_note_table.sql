create sequence if not exists note_sequence;
select setval('note_sequence', coalesce((select max(noteid) from note), 1)) FROM note;

alter table if exists note drop column code;
alter table if exists note drop column lastupdated;