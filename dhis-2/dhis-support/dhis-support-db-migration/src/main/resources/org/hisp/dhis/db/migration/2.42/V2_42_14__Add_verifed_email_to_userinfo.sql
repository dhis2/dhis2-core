ALTER TABLE userinfo ADD COLUMN if not exists verifiedemail VARCHAR(255);
ALTER TABLE userinfo ADD COLUMN if not exists emailverificationtoken VARCHAR(255);
ALTER TABLE userinfo ADD CONSTRAINT userinfo_verifiedemail_key UNIQUE (verifiedemail);
