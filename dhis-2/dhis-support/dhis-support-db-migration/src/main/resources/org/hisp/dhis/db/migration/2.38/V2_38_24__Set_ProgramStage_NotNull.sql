/* Update any null values to be set to the program stage's UID */
UPDATE programstage SET name = uid WHERE name IS NULL;

/* Set column to be not null */
ALTER TABLE programstage ALTER COLUMN name SET NOT NULL;