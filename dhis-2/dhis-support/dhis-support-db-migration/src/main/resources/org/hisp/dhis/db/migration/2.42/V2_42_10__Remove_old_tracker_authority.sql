-- delete unused authority. formerly used by old tracker and tracker capture
delete
from userroleauthorities
where authority = 'F_IGNORE_TRACKER_REQUIRED_VALUE_VALIDATION';
