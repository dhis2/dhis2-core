-- Migration related to https://dhis2.atlassian.net/browse/DHIS2-15725

-- This can be null. Now, in the case of multi-program, the program is set in the base object itself.
alter table if exists eventvisualization alter column programid drop not null;

-- Add a new column for the tracked entity type associates with the EventVisualization.
alter table eventvisualization add column if not exists trackedentitytypeid int8;

-- Adds a new FK to the trackedentitytype table. We drop and create it, so we can ensure idempotency.
alter table eventvisualization drop constraint if exists fk_evisualization_trackedentitytypeid;
alter table eventvisualization add constraint fk_evisualization_trackedentitytypeid foreign key (trackedentitytypeid) references trackedentitytype(trackedentitytypeid);
