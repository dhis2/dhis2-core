/*
Adds the "name" column to users as a computed column based on first and last name
to move named based filters into the DB.
This is important as the web API "query" search is a combination of multiple columns
one of which is name. Such combinations must stay all in DB to be correct.

Since both source columns are not null the name can and should also be not null
and the computation does not need to handle null.
*/
alter table userinfo
    add column if not exists name character varying(321) not null generated always as ( firstname || ' ' || surname ) stored;