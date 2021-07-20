DELETE FROM potentialduplicate WHERE teib IS NULL;
ALTER TABLE potentialduplicate ALTER COLUMN teib SET NOT NULL;