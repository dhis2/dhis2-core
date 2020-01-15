

-- Set cron to hourly for continuous job configurations

update jobconfiguration set "cronexpression" = '0 0 * ? * *' where "cronexpression" = '* * * * * ?';

-- Drop continuous execution column

alter table jobconfiguration drop column "continuousexecution";
