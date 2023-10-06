# Send Message

## Specific endpoints

### `sendMessages`

Send event or enrollment messages.

### `getScheduledSentMessage`

Get all of those scheduled messages which were sent successfully.

### `getProgramMessages`

Get program messages matching given query criteria.

## List of supported parameters

### `*.parameter.ou`

Get program messages for given set of OrganisationUnits

### `*.parameter.enrollment`

Get program messages for given enrollment

### `*.parameter.event`

Get program messages for given event

### `*.parameter.programInstace`

Get program messages for given enrollment

### `*.parameter.programStageInstace`

Get program messages for given event

### `*.parameter.afterDate`

Get program messages after given date

### `*.parameter.beforeDate`

Get program messages before given date

### `*.parameter.messageStatus`

Get program messages based on status for example SENT, SCHEDULED etc

### `*.parameter.page`

Number of pages to be loaded

### `*.parameter.pageSize`

Defines the number of elements to return for each page.

### `*.parameter.ProgramMessageBatch.programMessages`

Send batch of program messages