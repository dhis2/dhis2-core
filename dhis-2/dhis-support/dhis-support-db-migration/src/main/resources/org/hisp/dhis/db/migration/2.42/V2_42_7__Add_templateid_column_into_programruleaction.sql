-- Add the new column
ALTER TABLE programruleaction ADD COLUMN IF NOT EXISTS notificationtemplateid INTEGER NULL;

-- Update the new column with values from the old column
UPDATE programruleaction pra
SET notificationtemplateid = t.programnotificationtemplateid
    FROM programnotificationtemplate t
WHERE pra.templateuid = t.uid;

-- Remove templateUid

ALTER TABLE programruleaction DROP COLUMN IF EXISTS templateuid;

-- Conditionally add the foreign key constraint if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_notificationtemplate'
    ) THEN
ALTER TABLE programruleaction
    ADD CONSTRAINT fk_notificationtemplate
        FOREIGN KEY (notificationtemplateid) REFERENCES programnotificationtemplate(programnotificationtemplateid);
END IF;
END $$;