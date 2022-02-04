-- This script relates to the issue https://jira.dhis2.org/browse/TECH-905
-- Removes a duplicated FK/constraint that was added by mistake, in visualization_organisationunits.

alter table if exists visualization_organisationunits
    drop constraint if exists fkvisualization_organisationunits_organisationunitid;
