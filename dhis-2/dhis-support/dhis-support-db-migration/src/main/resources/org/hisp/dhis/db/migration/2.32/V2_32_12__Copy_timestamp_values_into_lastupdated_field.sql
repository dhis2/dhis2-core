-- Make sure there are no NULLs present in date column
update completedatasetregistration set date = now() where date is null;

-- Make sure there are no NULLs present in lastupdated column
update completedatasetregistration set lastupdated = date where lastupdated is null;

-- Make sure there are no NULLs present in lastupdated column
update datavalue set lastupdated = created where lastupdated is null;