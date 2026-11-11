-- Index the user groups of a user. The groups of the current user are read through a join on
-- usergroupmembers.userid, for the sharing filter and for the user details of a session, and the
-- only index of the table is its primary key (usergroupid, userid), so every such read scans the
-- whole table. A membership change makes every logged-in user read their groups again at once.
-- IF NOT EXISTS lets large installations create it beforehand with CREATE INDEX CONCURRENTLY
-- under the same name. DHIS2-22326.
CREATE INDEX IF NOT EXISTS "in_usergroupmembers_userid" ON "usergroupmembers" ("userid");
