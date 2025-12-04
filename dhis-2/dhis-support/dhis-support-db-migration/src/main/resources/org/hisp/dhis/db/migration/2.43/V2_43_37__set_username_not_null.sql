-- Update users with null username: generate random username and disable them
UPDATE userinfo
SET username = 'missing_uname_' || gen_random_uuid()::text,
    disabled = true
WHERE username IS NULL;

-- Add NOT NULL constraint to username column
ALTER TABLE userinfo ALTER COLUMN username SET NOT NULL;