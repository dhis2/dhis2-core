-- Add the new column
ALTER TABLE programruleaction ADD COLUMN notificationtemplateid INTEGER NULL;

-- Update the new column with values from the old column
UPDATE programruleaction pra
SET notificationtemplateid = t.id
    FROM programnotificationtemplate t
WHERE pra.templateuid = t.uid;

-- Drop the old column
ALTER TABLE programruleaction DROP COLUMN templateuid;

-- Add the foreign key constraint
ALTER TABLE programruleaction ADD CONSTRAINT fk_notificationtemplate FOREIGN KEY (notificationtemplateid)
    REFERENCES programnotificationtemplate(programnotificationtemplateid);
