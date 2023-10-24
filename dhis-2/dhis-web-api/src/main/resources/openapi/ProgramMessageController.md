    # Send Message

## Specific endpoints

### `sendMessages`

Send event or enrollment messages.

### `sendMessages.request.description`

Send batch of program messages

### `getScheduledSentMessage`

Get all of those scheduled messages which were sent successfully.

### `getProgramMessages`

Get program messages matching given query criteria.

### `getProgramMessages.parameter.ou`

Get program messages for given set of OrganisationUnits

### `getProgramMessages.parameter.messageStatus`

Get program messages based on message status

### `getProgramMessages.parameter.beforeDate`

Get program messages before given date

## Common for all endpoints

### `*.parameter.enrollment`

Get program messages for given enrollment

### `*.parameter.event`

Get program messages for given event

### `*.parameter.programInstace`

**DEPRECATED as of 2.41:** Use parameter enrollment instead

Get program messages for given enrollment

### `*.parameter.programStageInstace`

**DEPRECATED as of 2.41:** Use parameter event instead

Get program messages for given event

### `*.parameter.afterDate`

Get program messages after given date

### `*.parameter.page`

Defines which page number to return.

### `*.parameter.pageSize`

Defines the number of elements to return for each page.