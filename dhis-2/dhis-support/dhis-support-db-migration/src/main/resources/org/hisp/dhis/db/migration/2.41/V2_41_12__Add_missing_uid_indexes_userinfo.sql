/* add not null */
UPDATE messageconversation SET uid = messageconversationid WHERE uid IS NULL;
ALTER TABLE messageconversation ALTER COLUMN uid SET NOT NULL;

/* Only alter table if uid is effectively unique */
CREATE FUNCTION make_messageconversation_uid_unique() RETURNS VOID AS
$$
BEGIN
    PERFORM 1 FROM messageconversation GROUP BY uid HAVING COUNT(*) > 1;
    IF NOT FOUND THEN
        DROP INDEX IF EXISTS uk_messageconversation_uid;
        CREATE UNIQUE INDEX uk_messageconversation_uid ON public.messageconversation (uid);
    END IF;
END;
$$ LANGUAGE plpgsql;

/* run the update */
SELECT make_messageconversation_uid_unique();
/* clean up the update function */
DROP FUNCTION IF EXISTS make_messageconversation_uid_unique();