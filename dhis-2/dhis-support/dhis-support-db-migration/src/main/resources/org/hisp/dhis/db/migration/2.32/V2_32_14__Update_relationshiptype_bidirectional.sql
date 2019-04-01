alter table relationshiptype
  add column bidirectional boolean;

alter table relationshiptype
  add column fromToName character varying(255);

alter table relationshiptype
  add column toFromName character varying(255);

update relationshiptype set fromToName = name where fromToName is null;

update relationshiptype set bidirectional = false where bidirectional is null;

alter table relationshiptype
alter column bidirectional set not null;