alter table chart
add column if not exists userorgunittype character varying(20);

alter table eventchart
add column if not exists userorgunittype character varying(20);

alter table eventreport
add column if not exists userorgunittype character varying(20);

alter table mapview
add column if not exists userorgunittype character varying(20);

alter table reporttable
add column if not exists userorgunittype character varying(20);
