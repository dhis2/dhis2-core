-- Substitute job configurations with PROGRAM_DATA_SYNC job type with 2 new job configurations of job types
-- TRACKER_PROGRAMS_DATA_SYNC and EVENT_PROGRAMS_DATA_SYNC

-- Create temporary sequences
create temp sequence tracker_program_data_sync_seq increment by 1 start with 1;
create temp sequence event_program_data_sync_seq increment by 1 start with 1;

-- Create a new job configurations with the same settings as the existing Program Data sync job configurations
insert into jobconfiguration ( jobconfigurationid, uid, code, created, lastupdated, lastupdatedby, name,
                              cronexpression, jobtype, jobstatus, lastexecutedstatus, lastexecuted,
                              lastruntimeexecution, nextexecutiontime, continuousexecution, enabled,
                              leaderonlyjob, jsonbjobparameters )
    ( select nextval('hibernate_sequence'), generate_uid(), code, created, lastupdated, lastupdatedby,
             'Tracker programs data synchronization - ' || nextval('tracker_program_data_sync_seq'),
             cronexpression, 'TRACKER_PROGRAMS_DATA_SYNC', jobstatus, lastexecutedstatus, lastexecuted,
             lastruntimeexecution, nextexecutiontime, continuousexecution, enabled,
             leaderonlyjob, jsonbjobparameters from jobconfiguration where jobtype='PROGRAM_DATA_SYNC' );

insert into jobconfiguration ( jobconfigurationid, uid, code, created, lastupdated, lastupdatedby, name,
                               cronexpression, jobtype, jobstatus, lastexecutedstatus, lastexecuted,
                               lastruntimeexecution, nextexecutiontime, continuousexecution, enabled,
                               leaderonlyjob, jsonbjobparameters )
    ( select nextval('hibernate_sequence'), generate_uid(), code, created, lastupdated, lastupdatedby,
             'Event programs data synchronization - ' || nextval('event_program_data_sync_seq'),
             cronexpression, 'EVENT_PROGRAMS_DATA_SYNC', jobstatus, lastexecutedstatus, lastexecuted,
             lastruntimeexecution, nextexecutiontime, continuousexecution, enabled,
             leaderonlyjob, jsonbjobparameters from jobconfiguration where jobtype='PROGRAM_DATA_SYNC' );

-- Remove old job configurations with PROGRAM_DATA_SYNC job type
delete from jobconfiguration where jobtype='PROGRAM_DATA_SYNC';

-- Remove temporary sequences
drop sequence tracker_program_data_sync_seq;
drop sequence event_program_data_sync_seq;