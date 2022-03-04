create unique index if not exists in_reservedvalue_value_generation on reservedvalue using btree (ownerobject, owneruid, key, lower (value));
