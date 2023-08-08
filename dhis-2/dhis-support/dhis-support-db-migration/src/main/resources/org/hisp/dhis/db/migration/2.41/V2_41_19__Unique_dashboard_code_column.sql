alter table dashboard drop constraint if exists dashboard_unique_code;
alter table dashboard add constraint dashboard_unique_code unique (code);
