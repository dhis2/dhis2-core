ALTER TABLE trackedentityinstance ADD COLUMN if not exists potentialDuplicate BOOLEAN DEFAULT FALSE;

UPDATE
    trackedentityinstance
SET
    potentialduplicate = true
WHERE
        uid IN (
        SELECT
            teia
        FROM
            potentialduplicate p
        WHERE
            teib IS NULL
    );