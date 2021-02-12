create index if not exists programinstance_trackedentityinstance on programinstance (trackedentityinstanceid);
create index if not exists relationshipitem_trackedentityinstance on relationshipitem (trackedentityinstanceid);
create index if not exists relationshipitem_programinstance on relationshipitem (programinstanceid);
create index if not exists relationshipitem_programstageinstance on relationshipitem (programstageinstanceid);