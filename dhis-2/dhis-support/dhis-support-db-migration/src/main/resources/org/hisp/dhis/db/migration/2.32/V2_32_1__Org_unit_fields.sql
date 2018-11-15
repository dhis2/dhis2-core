

alter table eventreport
add column if not exists orgunitfield character varying(255);

alter table eventchart
add column if not exists orgunitfield character varying(255);

