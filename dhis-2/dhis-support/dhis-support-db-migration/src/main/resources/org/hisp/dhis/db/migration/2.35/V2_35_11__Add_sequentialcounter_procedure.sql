-- remove primary key on id
alter table sequentialnumbercounter drop constraint sequentialnumbercounter_pkey;
-- create composite primary key
alter table sequentialnumbercounter add constraint seqnumcount_pkey primary key (owneruid, key);

CREATE OR REPLACE FUNCTION incrementSequentialCounter(counter_owner text, counter_key text, size integer) RETURNS integer AS $$
	DECLARE
		current_counter integer;
	BEGIN
        INSERT INTO sequentialnumbercounter  (id, owneruid, key, counter)
        VALUES(nextval('hibernate_sequence'), counter_owner, counter_key, (1 + size) )

        ON CONFLICT (owneruid, key)
            DO
                UPDATE SET counter = (sequentialnumbercounter.counter + size)
                WHERE sequentialnumbercounter.owneruid = counter_owner
                AND sequentialnumbercounter.key = counter_key

        RETURNING counter INTO current_counter;

        RETURN current_counter;

	END;
$$ LANGUAGE plpgsql;