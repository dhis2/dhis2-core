alter table programstageinstance drop CONSTRAINT IF EXISTS fk_programstageinstance_assigneduserid;

ALTER TABLE programstageinstance
ADD CONSTRAINT fk_programstageinstance_assigneduserid FOREIGN KEY (assigneduserid)
REFERENCES userinfo(userinfoid);
