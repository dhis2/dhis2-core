/* update old data entry app authority to new data entry app authority */
UPDATE userroleauthorities SET authority = 'M_dhis-web-aggregate-data-entry' WHERE authority = 'M_dhis-web-dataentry';

/* Deduplicate */
DELETE FROM userroleauthorities a
WHERE a.ctid <> (SELECT min(b.ctid) FROM userroleauthorities b WHERE a.userroleid = b.userroleid AND a.authority = b.authority);

/* add unique constraint */
ALTER TABLE userroleauthorities ADD CONSTRAINT userroleauthorities_unique_key UNIQUE (userroleid, authority);