CREATE OR REPLACE FUNCTION incrementSequentialCounter(counter_owner text, counter_key text, i integer) RETURNS integer AS $$
	DECLARE
		-- Get the initial count, or 0 if it doesnt exist
		current_counter integer;
	BEGIN
		-- Lock the row, so noone else gets an old count.
		LOCK TABLE sequentialnumbercounter IN ROW EXCLUSIVE MODE;
		-- Assign the variable
		SELECT SNC.counter
		INTO current_counter
		FROM sequentialnumbercounter SNC
		WHERE SNC.owneruid = counter_owner
		AND SNC.key = counter_key;
		-- If it's 0, we add a new row
		IF current_counter IS NULL THEN
			INSERT INTO sequentialnumbercounter (id, owneruid, key, counter)
			VALUES(nextval('hibernate_sequence'), counter_owner, counter_key, (1 + i));
			RETURN 1;
		END IF;
		-- If it's not null, we update with the increment
		UPDATE sequentialnumbercounter
		SET counter = counter + i
		WHERE owneruid = counter_owner
		AND key = counter_key;
		RETURN current_counter;
	END;
$$ LANGUAGE plpgsql;