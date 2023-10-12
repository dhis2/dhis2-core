alter table relationship add column if not exists "createdatclient" timestamp;

update relationship set createdatclient = created where createdatclient IS NULL;
