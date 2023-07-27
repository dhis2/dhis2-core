# Send Message

## Specific endpoints

### `sendMessages`

Send event or enrollment messages.

### `getScheduledSentMessage`

Get all of those scheduled messages which were sent successfully.

### `getProgramMessages`

Get programMessages matching given query criteria.

## List of supported parameters

### `*.parameter.ou`

Get programMessages for given set of OrganisationUnits

### `*.parameter.enrollment`

Get programMessages for given enrollment

### `*.parameter.event`

Get programMessages for given event

### `*.parameter.programInstace`

Get programMessages for given enrollment

### `*.parameter.programStageInstace`

Get programMessages for given event

### `*.parameter.afterDate`

Get programMessages after given date

### `*.parameter.beforeDate`

Get programMessages before given date

### `*.parameter.messageStatus`

Get programMessages based on status for example SENT, SCHEDULED etc

### `*.parameter.page`

Number of pages to be loaded

### `*.parameter.pageSize`

Number of records per page

### `*.parameter.ProgramMessageBatch.programMessages`

Send batch of programMessages