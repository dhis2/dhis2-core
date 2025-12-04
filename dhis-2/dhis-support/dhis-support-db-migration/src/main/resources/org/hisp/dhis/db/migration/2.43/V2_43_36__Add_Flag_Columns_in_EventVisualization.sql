-- This script relates to the task https://jira.dhis2.org/browse/DHIS2-20531

-- Add new column "hideemptycolumns"
alter table eventvisualization add column if not exists hideemptycolumns boolean;
update eventvisualization set hideemptycolumns = false;
alter table eventvisualization alter column hideemptycolumns set not null;

-- Add new column "fixcolumnheaders"
alter table eventvisualization add column if not exists fixcolumnheaders boolean;
update eventvisualization set fixcolumnheaders = false;
alter table eventvisualization alter column fixcolumnheaders set not null;

-- Add new column "fixrowheaders"
alter table eventvisualization add column if not exists fixrowheaders boolean;
update eventvisualization set fixrowheaders = false;
alter table eventvisualization alter column fixrowheaders set not null;
