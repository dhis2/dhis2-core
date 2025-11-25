-- Migration related to DHIS2-19463.


-- Rename table.
alter table if exists intepretation_likedby rename to interpretation_likedby;

-- Rename FK.
alter table name rename constraint "fk_intepretation_likedby_userid" to "fk_interpretation_likedby_userid";
