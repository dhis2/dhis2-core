
-- add column uid to incomingsms and outboundsms table
alter table incomingsms add column if not exists "uid" varchar(255);
alter table outbound_sms add column if not exists "uid" varchar(255);

-- change id column type from integer to long
alter table incomingsms alter column id type bigint;
alter table outbound_sms alter column id type bigint;

-- update uid column with randomly generated uid values
update incomingsms set uid = generate_uid() where uid is null;
update outbound_sms set uid = generate_uid() where uid is null;