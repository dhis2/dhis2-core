-- Abort if any tracked entity attribute value has an encrypted value but no plain text value,
-- as removing the encryptedvalue column would cause irreversible data loss.
DO $$
DECLARE
    encrypted_only_count INTEGER;
BEGIN
    SELECT COUNT(*)
    INTO encrypted_only_count
    FROM trackedentityattributevalue
    WHERE encryptedvalue IS NOT NULL
        AND (value IS NULL OR value = '');

    IF encrypted_only_count > 0 THEN
        RAISE EXCEPTION
            'Migration aborted: % row(s) in trackedentityattributevalue have an encrypted value '
            'but no plain text value. Decrypt these attribute values before running this migration.',
            encrypted_only_count;
    END IF;
END;
$$;

-- Preserve analytics exclusion intent: attributes that were confidential should skip analytics.
UPDATE trackedentityattribute
SET skipanalytics = true
WHERE confidential = true
    AND skipanalytics = false;

ALTER TABLE trackedentityattribute
DROP COLUMN IF EXISTS confidential;

ALTER TABLE trackedentityattributevalue
DROP COLUMN IF EXISTS encryptedvalue;
