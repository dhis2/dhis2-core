ALTER TABLE relationshiptype
  ADD COLUMN bidirectional BOOLEAN;

ALTER TABLE relationshiptype
  ADD COLUMN fromToName CHARACTER VARYING(255);

ALTER TABLE relationshiptype
  ADD COLUMN toFromName CHARACTER VARYING(255);

UPDATE relationshiptype SET fromToName = name;

UPDATE relationshiptype SET bidirectional = false;

ALTER TABLE relationshiptype
ALTER COLUMN bidirectional SET NOT NULL;