-- Move message column from varchar to text to store larger messages outcome
ALTER TABLE outbound_sms ALTER COLUMN message TYPE text;

