update sqlview set type='VIEW' where type is null;
update sqlview set cachestrategy='RESPECT_SYSTEM_SETTING' where cachestrategy is null;

ALTER TABLE sqlview ALTER COLUMN type SET NOT NULL;
ALTER TABLE sqlview ALTER COLUMN cachestrategy SET NOT NULL;