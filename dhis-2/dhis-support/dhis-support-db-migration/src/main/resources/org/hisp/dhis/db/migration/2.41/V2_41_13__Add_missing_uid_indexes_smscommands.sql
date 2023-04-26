/* add not null */
UPDATE smscommands SET uid = smscommandid WHERE uid IS NULL;
ALTER TABLE smscommands ALTER COLUMN uid SET NOT NULL;

/* Only alter table if uid is effectively unique */
CREATE FUNCTION make_smscommands_uid_unique() RETURNS VOID AS
$$
BEGIN
    PERFORM 1 FROM smscommands GROUP BY uid HAVING COUNT(*) > 1;
    IF NOT FOUND THEN
        DROP INDEX IF EXISTS uk_smscommands_uid;
        CREATE UNIQUE INDEX uk_smscommands_uid ON public.smscommands (uid);
    END IF;
END;
$$ LANGUAGE plpgsql;

/* run the update */
SELECT make_smscommands_uid_unique();
/* clean up the update function */
DROP FUNCTION IF EXISTS make_smscommands_uid_unique();