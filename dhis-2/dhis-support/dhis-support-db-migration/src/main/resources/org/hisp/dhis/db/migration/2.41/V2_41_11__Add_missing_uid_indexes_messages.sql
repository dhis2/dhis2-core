/* add not null */
UPDATE message SET uid = messageid WHERE uid IS NULL;
ALTER TABLE message ALTER COLUMN uid SET NOT NULL;

/* Only alter table if uid is effectively unique */
CREATE FUNCTION make_message_uid_unique() RETURNS VOID AS
$$
BEGIN
    PERFORM 1 FROM message GROUP BY uid HAVING COUNT(*) > 1;
    IF NOT FOUND THEN
        DROP INDEX IF EXISTS uk_message_uid;
        CREATE UNIQUE INDEX uk_message_uid ON public.message (uid);
    END IF;
END;
$$ LANGUAGE plpgsql;

/* run the update */
SELECT make_message_uid_unique();
/* clean up the update function */
DROP FUNCTION IF EXISTS make_message_uid_unique();