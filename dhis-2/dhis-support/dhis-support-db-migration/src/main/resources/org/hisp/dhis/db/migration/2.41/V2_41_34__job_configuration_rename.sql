-- delete existing rows with the enum as it got renamed
-- a new row will be inserted automatically at startup
delete from jobconfiguration where jobtype = 'HEARTBEAT';