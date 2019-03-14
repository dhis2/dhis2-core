-- Make sure there are no NULLs present in lastupdated column
update datavalue set lastupdated = created where lastupdated is null;