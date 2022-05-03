ALTER TABLE relationship ADD COLUMN IF NOT EXISTS deleted boolean DEFAULT false;

ALTER TABLE relationship drop constraint fk_relationship_from_relationshipitemid;
ALTER TABLE relationship add constraint fk_relationship_from_relationshipitemid foreign key (from_relationshipitemid) references relationshipitem(relationshipitemid) on delete cascade;

ALTER TABLE relationship drop constraint fk_relationship_to_relationshipitemid;
ALTER TABLE relationship add constraint fk_relationship_to_relationshipitemid foreign key (to_relationshipitemid) references relationshipitem(relationshipitemid) on delete cascade;

alter table relationshipitem drop constraint  fk_relationshipitem_relationshipid;
alter table relationshipitem add constraint  fk_relationshipitem_relationshipid foreign key(relationshipid) references relationship(relationshipid) on delete cascade;