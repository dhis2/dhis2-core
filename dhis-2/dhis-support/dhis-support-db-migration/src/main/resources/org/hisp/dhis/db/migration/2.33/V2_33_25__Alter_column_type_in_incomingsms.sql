
-- Alter text column in incomingsms table to allow more than 255 characters
alter table incomingsms alter column text TYPE text;