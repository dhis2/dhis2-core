-- Update ALL status to OPEN if any have been created with ALL
update potentialduplicate set status = 'OPEN' where status = 'ALL';