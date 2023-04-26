/* add not null */
UPDATE interpretationcomment SET uid = interpretationcommentid WHERE uid IS NULL;
ALTER TABLE interpretationcomment ALTER COLUMN uid SET NOT NULL;

/* Only alter table if uid is effectively unique */
CREATE FUNCTION make_interpretationcomment_uid_unique() RETURNS VOID AS
$$
BEGIN
    PERFORM 1 FROM interpretationcomment GROUP BY uid HAVING COUNT(*) > 1;
    IF NOT FOUND THEN
        DROP INDEX IF EXISTS uk_interpretationcomment_uid;
        CREATE UNIQUE INDEX uk_interpretationcomment_uid ON public.interpretationcomment (uid);
    END IF;
END;
$$ LANGUAGE plpgsql;

/* run the update */
SELECT make_interpretationcomment_uid_unique();
/* clean up the update function */
DROP FUNCTION IF EXISTS make_interpretationcomment_uid_unique();