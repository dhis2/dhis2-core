-- rename authority to describe that it bypasses org unit scope and program ownership
update userroleauthorities
set authority = 'F_TRACKER_SEARCH_ALL_ORG_UNITS_IGNORING_SCOPE_AND_OWNERSHIP'
where authority = 'F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS';
