-- Expired values Job has been merged with used reserved values Job
delete from jobconfiguration where jobtype = 'REMOVE_EXPIRED_RESERVED_VALUES';