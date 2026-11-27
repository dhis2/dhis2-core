# Data Integrity

## Specific endpoints

### `getSummariesMetrics`
Gives the latest summary result of each data integrity check in the Prometheus text format.
The response includes results of any age, so a monitoring system keeps the series between check runs.

## Common for all endpoints

### `*.parameter.maxAge`
Maximum age of a summary result in hours. The response only includes results of checks that finished within the last `maxAge` hours.
Use `0` for no limit. The default is `1`. A negative value gives error `E2005`.
