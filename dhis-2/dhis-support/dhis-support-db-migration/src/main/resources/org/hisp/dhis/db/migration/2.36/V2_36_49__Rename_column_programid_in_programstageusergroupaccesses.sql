DO $$
BEGIN
  IF EXISTS(SELECT *
    FROM information_schema.columns
    WHERE table_name='programstageusergroupaccesses' and column_name='programid')
  THEN
alter table programstageusergroupaccesses rename COLUMN programid to programstageid;
END IF;
END $$;


alter table programstageinstance drop CONSTRAINT IF EXISTS fk_programstageinstance_assigneduserid;

ALTER TABLE programstageinstance
ADD CONSTRAINT fk_programstageinstance_assigneduserid FOREIGN KEY (assigneduserid)
REFERENCES userinfo(userinfoid);