create index if not exists in_programinstance_trackedentityinstance on programinstance (trackedentityinstanceid);
create index if not exists in_relationshipitem_trackedentityinstance on relationshipitem (trackedentityinstanceid);
create index if not exists in_relationshipitem_programinstance on relationshipitem (programinstanceid);
create index if not exists in_relationshipitem_programstageinstance on relationshipitem (programstageinstanceid);
