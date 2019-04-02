alter table relationshiptype
  add column if not exists bidirectional boolean;

alter table relationshiptype
  add column if not exists  fromToName character varying(255);

alter table relationshiptype
  add column if not exists  toFromName character varying(255);

update relationshiptype set fromToName = name where fromToName is null;

update relationshiptype set bidirectional = false where bidirectional is null;

alter table relationshiptype
  alter column bidirectional set not null;

alter table relationshiptype
  alter column fromToName set not null;