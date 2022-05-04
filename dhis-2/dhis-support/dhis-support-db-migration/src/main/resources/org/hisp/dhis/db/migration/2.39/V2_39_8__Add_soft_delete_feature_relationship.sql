ALTER TABLE relationship ADD COLUMN IF NOT EXISTS deleted boolean DEFAULT false;

ALTER TABLE relationship DROP CONSTRAINT IF EXISTS fk_relationship_from_relationshipitemid;
ALTER TABLE relationship ADD CONSTRAINT fk_relationship_from_relationshipitemid FOREIGN KEY (from_relationshipitemid) REFERENCES relationshipitem(relationshipitemid) ON DELETE CASCADE;

ALTER TABLE relationship DROP CONSTRAINT IF EXISTS  fk_relationship_to_relationshipitemid;
ALTER TABLE relationship ADD CONSTRAINT fk_relationship_to_relationshipitemid FOREIGN KEY (to_relationshipitemid) REFERENCES relationshipitem(relationshipitemid) ON DELETE CASCADE;

ALTER TABLE relationshipitem DROP CONSTRAINT IF EXISTS fk_relationshipitem_relationshipid;
ALTER TABLE relationshipitem ADD CONSTRAINT  fk_relationshipitem_relationshipid FOREIGN KEY(relationshipid) REFERENCES relationship(relationshipid) ON DELETE CASCADE;