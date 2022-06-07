ALTER TABLE relationshiptype ADD COLUMN IF NOT EXISTS referral BOOLEAN;

UPDATE relationshiptype SET referral = FALSE WHERE referral IS NULL;

ALTER TABLE relationshiptype ALTER COLUMN referral SET DEFAULT false;

ALTER TABLE relationshiptype ALTER COLUMN referral SET NOT NULL;

