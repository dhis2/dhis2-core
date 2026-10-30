-- Remove Push Analysis feature completely

-- Delete job configurations of type PUSH_ANALYSIS
delete from jobconfiguration where jobtype = 'PUSH_ANALYSIS';

-- Drop the junction table for push analysis recipient user groups
drop table if exists pushanalysisrecipientusergroups;

-- Drop the main push analysis table
drop table if exists pushanalysis;

-- Delete file resources associated with push analysis
delete from fileresource where domain = 'PUSH_ANALYSIS';
