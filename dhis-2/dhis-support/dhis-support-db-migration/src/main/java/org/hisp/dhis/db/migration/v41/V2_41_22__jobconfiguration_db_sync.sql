-- applies all changes needed to use jobconfiguration as the single source of truth
-- and TX mechanism of the job scheduling

-- cleanup
delete from jobconfiguration where jobtype = 'LEADER_RENEWAL';
delete from jobconfiguration where jobtype = 'LEADER_ELECTION';

-- schema changes
alter table jobconfiguration
    drop column if exists lastruntimeexecution,
    add column if not exists schedulingtype varchar(12) not null default 'ONCE_ASAP',
    add column if not exists lastalive timestamp,
    add column if not exists lastfinished timestamp,
    add column if not exists cancel boolean not null default false,
    add column if not exists progress jsonb;

-- fix CRON default for CONTINUOUS_ANALYTICS_TABLE job which is the only one using delay
update jobconfiguration set schedulingtype = 'FIXED_DELAY' where jobtype = 'CONTINUOUS_ANALYTICS_TABLE';

-- job status as required column
update jobconfiguration set jobstatus = 'DISABLED' where jobstatus is null;
alter table jobconfiguration alter column jobstatus set not null;

delete from jobconfiguration where jobtype is null;
alter table jobconfiguration alter column jobtype set not null;

-- last executed status as required column
update jobconfiguration set lastexecutedstatus = 'NOT_STARTED' where lastexecutedstatus is null;
alter table jobconfiguration alter column lastexecutedstatus set not null;