alter table programstageusergroupaccesses rename COLUMN programid to programstageid;

alter table programstageinstance drop CONSTRAINT fk_programstageinstance_assigneduserid;

ALTER TABLE programstageinstance
ADD CONSTRAINT fk_programstageinstance_assigneduserid FOREIGN KEY (assigneduserid)
REFERENCES userinfo(userinfoid);