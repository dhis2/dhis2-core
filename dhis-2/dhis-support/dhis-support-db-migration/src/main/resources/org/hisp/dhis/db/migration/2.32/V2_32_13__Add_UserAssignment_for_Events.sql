ALTER TABLE programstageinstance
ADD COLUMN assigneduserid BIGINT;

ALTER TABLE programstageinstance
ADD CONSTRAINT fk_programstageinstance_assigneduserid FOREIGN KEY (assigneduserid)
REFERENCES users(userid);


ALTER TABLE programstage
ADD COLUMN enableuserassignment boolean;

UPDATE programstage SET enableuserassignment = false WHERE programstage.enableuserassignment IS NULL;

ALTER TABLE programstage
ALTER COLUMN enableuserassignment SET NOT NULL;