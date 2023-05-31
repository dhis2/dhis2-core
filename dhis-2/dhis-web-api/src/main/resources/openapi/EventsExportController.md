# Get Events

## `getEvents`

Get events matching given query parameters.

## `getEventByUid`

Get an event with given UID.

## Common for all endpoints

### `*.parameter.EventRequestParams.program`

### `*.parameter.EventRequestParams.programStage`

### `*.parameter.EventRequestParams.programStatus`

### `*.parameter.EventRequestParams.followUp`

Get events with given follow-up status of the instance for the given program.

### `*.parameter.EventRequestParams.trackedEntity`

Get events of tracked entity with given UID.

### `*.parameter.EventRequestParams.orgUnit`

Get events owned by given `orgUnit`.

### `*.parameter.EventRequestParams.ouMode`

Get events using given organisation unit selection mode.

### `*.parameter.EventRequestParams.assignedUserMode`

### `*.parameter.EventRequestParams.assignedUsers`

`<user1-uid>[,<user2-uid>...]`

Get events that are assigned to the given user(s). Specifying `assignedUsers` is only valid if `assignedUserMode` is
either `PROVIDED` or not specified.

### `*.parameter.EventRequestParams.assignedUser`

**DEPRECATED as of 2.41:** Use parameter `assignedUsers` instead where UIDs have to be separated by comma!

`<user1-uid>[;<user2-uid>...]`

Get events that are assigned to the given user(s). Specifying `assignedUser` is only valid if `assignedUserMode` is
either `PROVIDED` or not specified.

### `*.parameter.EventRequestParams.occurredAfter`

Get events that occurred after given date.

### `*.parameter.EventRequestParams.occurredBefore`

Get events that occurred before given date.

### `*.parameter.EventRequestParams.scheduledAfter`

Get events that are scheduled after given date.

### `*.parameter.EventRequestParams.scheduledBefore`

Get events that are scheduled before given date.

### `*.parameter.EventRequestParams.updatedAfter`

Get events updated after given date.

### `*.parameter.EventRequestParams.updatedWithin`

Get events updated since given ISO-8601 duration.

### `*.parameter.EventRequestParams.enrollmentEnrolledAfter`

Get events with enrollments that were enrolled after given date.

### `*.parameter.EventRequestParams.enrollmentEnrolledBefore`

Get events with enrollments that were enrolled before given date.

### `*.parameter.EventRequestParams.status`

### `*.parameter.EventRequestParams.attributeCategoryCombo`

### `*.parameter.EventRequestParams.attributeCc`

**DEPRECATED as of 2.41:** Use parameter `attributeCategoryCombo` instead.

### `*.parameter.EventRequestParams.attributeCategoryOptions`

`<attributeCategoryOption1-uid>[,<attributeCategoryOption2-uid>...]`

### `*.parameter.EventRequestParams.attributeCos`

`<attributeCategoryOption1-uid>[;<attributeCategoryOption2-uid>...]`

**DEPRECATED as of 2.41:** Use parameter `attributeCategoryOptions` instead where UIDs have to be separated by comma!

### `*.parameter.EventRequestParams.includeDeleted`

Get soft-deleted events by specifying `includeDeleted=true`. Soft-deleted events are excluded by default.

### `*.parameter.EventRequestParams.events`

`<event1-uid>[,<event2-uid>...]`

Get events with given UID(s).

### `*.parameter.EventRequestParams.event`

**DEPRECATED as of 2.41:** Use parameter `events` instead where UIDs have to be separated by comma!

`<event1-uid>[;<event2-uid>...]`

Get events with given UID(s).

### `*.parameter.EventRequestParams.skipEventId`

### `*.parameter.EventRequestParams.order`

`<propertyName1:sortDirection>[,<propertyName2:sortDirection>...]`

Get events in given order. Valid `sortDirection`s are `asc` and `desc`. `propName` is case-sensitive, `sortDirection`
is case-insensitive.

Supported properties are `assignedUser`, `assignedUserDisplayName`, `attributeOptionCombo`, `completedAt`,
`completedBy`, `createdAt`, `createdBy`, `deleted`, `enrolledAt`, `enrollment`, `enrollmentStatus`, `event`, `followup`,
`occurredAt`, `orgUnit`, `orgUnitName`, `program`, `programStage`, `scheduleAt`, `status`, `storedBy`, `trackedEntity`,
`updatedAt`, `updatedBy`.
