create index if not exists in_programinstance_trackedentityinstanceid on programinstance (trackedentityinstanceid);
create index if not exists in_relationshipitem_trackedentityinstanceid on relationshipitem (trackedentityinstanceid);
create index if not exists in_relationshipitem_programinstanceid on relationshipitem (programinstanceid);
create index if not exists in_relationshipitem_programstageinstanceid on relationshipitem (programstageinstanceid);
