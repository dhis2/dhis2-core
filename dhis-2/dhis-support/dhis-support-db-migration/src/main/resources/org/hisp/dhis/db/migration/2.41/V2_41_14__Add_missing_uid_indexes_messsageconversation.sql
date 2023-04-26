/* add not null */
UPDATE userinfo SET uid = username WHERE uid IS NULL;
ALTER TABLE userinfo ALTER COLUMN uid SET NOT NULL;

/* Only alter table if uid is effectively unique */
CREATE FUNCTION make_userinfo_uid_unique() RETURNS VOID AS
$$
BEGIN
    PERFORM 1 FROM userinfo GROUP BY uid HAVING COUNT(*) > 1;
    IF NOT FOUND THEN
        DROP INDEX IF EXISTS uk_userinfo_uid;
        CREATE UNIQUE INDEX uk_userinfo_uid ON public.userinfo (uid);
    END IF;
END;
$$ LANGUAGE plpgsql;

/* run the update */
SELECT make_userinfo_uid_unique();

/* clean up the update function */
DROP FUNCTION IF EXISTS make_userinfo_uid_unique();