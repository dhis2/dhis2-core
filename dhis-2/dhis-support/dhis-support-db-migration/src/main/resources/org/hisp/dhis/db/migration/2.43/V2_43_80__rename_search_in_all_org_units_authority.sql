-- rename authority to describe that it reads tracker data overriding org unit scope and program ownership
update userroleauthorities
set authority = 'F_TRACKER_READ_ALL_ORG_UNITS_OVERRIDING_SCOPE_AND_OWNERSHIP'
where authority = 'F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS';
