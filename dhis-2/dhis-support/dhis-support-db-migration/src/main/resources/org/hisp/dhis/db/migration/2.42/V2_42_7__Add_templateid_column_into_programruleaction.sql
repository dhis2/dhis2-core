-- Add the new column
ALTER TABLE programruleaction ADD COLUMN IF NOT EXISTS notificationtemplateid INTEGER NULL;

-- Update the new column with values from the old column
UPDATE programruleaction pra
SET notificationtemplateid = t.programnotificationtemplateid
    FROM programnotificationtemplate t
WHERE pra.templateuid = t.uid;

-- Add the foreign key constraint
ALTER TABLE programruleaction ADD CONSTRAINT fk_notificationtemplate FOREIGN KEY (notificationtemplateid)
    REFERENCES programnotificationtemplate(programnotificationtemplateid);
