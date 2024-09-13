--Fractional expiry days are now supported in the dataset expiry days field.
ALTER TABLE dataset ALTER COLUMN expirydays TYPE DOUBLE PRECISION USING expirydays::DOUBLE PRECISION;