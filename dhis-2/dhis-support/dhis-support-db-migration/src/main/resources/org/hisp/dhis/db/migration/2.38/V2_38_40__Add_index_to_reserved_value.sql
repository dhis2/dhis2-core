create unique index if not exists in_reservedvalue_value_generation on reservedvalue using btree (ownerobject, owneruid, key, lower (value));
ALTER TABLE ONLY reservedvalue DROP CONSTRAINT IF EXISTS uk_2utuk3clxif3qi4icy859kdrb;
