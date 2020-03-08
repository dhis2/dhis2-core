
-- Update lastupdatedby column on completedatasetregistration for existing data

update completedatasetregistration set lastupdatedby = storedby where lastupdatedby is null;
