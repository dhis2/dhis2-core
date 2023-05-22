# Get Events

## `getEvents`

Get events matching given query parameters.

### `getEvents.parameter.program`

### `getEvents.parameter.programStage`

### `getEvents.parameter.programStatus`

### `getEvents.parameter.followUp`

Get events with given follow-up status of the instance for the given program.

### `getEvents.parameter.trackedEntity`

Get events of tracked entity with given UID.

### `getEvents.parameter.orgUnit`

Get events owned by given `orgUnit`.

### `getEvents.parameter.ouMode`

Get events using given organisation unit selection mode.

### `getEvents.parameter.assignedUserMode`

### `getEvents.parameter.assignedUsers`

`<user1-uid>[,<user2-uid>...]`

Get events that are assigned to the given user(s). Specifying `assignedUsers` is only valid if `assignedUserMode` is
either `PROVIDED` or not specified.

### `getEvents.parameter.assignedUser`

**DEPRECATED as of 2.41:** Use parameter `assignedUsers` instead where UIDs have to be separated by comma!

`<user1-uid>[;<user2-uid>...]`

Get events that are assigned to the given user(s). Specifying `assignedUser` is only valid if `assignedUserMode` is
either `PROVIDED` or not specified.

### `getEvents.parameter.occurredAfter`

Get events that occurred after given date.

### `getEvents.parameter.occurredBefore`

Get events that occurred before given date.

### `getEvents.parameter.scheduledAfter`

Get events that are scheduled after given date.

### `getEvents.parameter.scheduledBefore`

Get events that are scheduled before given date.

### `getEvents.parameter.updatedAfter`

Get events updated after given date.

### `getEvents.parameter.updatedWithin`

Get events updated since given ISO-8601 duration.

### `getEvents.parameter.enrollmentEnrolledAfter`

Get events with enrollments that were enrolled after given date.

### `getEvents.parameter.enrollmentEnrolledBefore`

Get events with enrollments that were enrolled before given date.

### `getEvents.parameter.status`

### `getEvents.parameter.attributeCc`

### `getEvents.parameter.attributeCos`

### `getEvents.parameter.includeDeleted`

Get soft-deleted events by specifying `includeDeleted=true`. Soft-deleted events are excluded by default.

### `getEvents.parameter.event`

`<event1-uid>[;<event2-uid>...]`

Get events with given UID(s).

### `getEvents.parameter.skipEventId`

### `getEvents.parameter.order`

`<propertyName1:sortDirection>[,<propertyName2:sortDirection>...]`

Get events in given order. Valid `sortDirection`s are `asc` and `desc`. `propName` is case-sensitive, `sortDirection`
is case-insensitive.

Supported properties are `assignedUser`, `assignedUserDisplayName`, `attributeOptionCombo`, `completedAt`,
`completedBy`, `createdAt`, `createdBy`, `deleted`, `enrolledAt`, `enrollment`, `enrollmentStatus`, `event`, `followup`,
`occurredAt`, `orgUnit`, `orgUnitName`, `program`, `programStage`, `scheduleAt`, `status`, `storedBy`, `trackedEntity`,
`updatedAt`, `updatedBy`.

## `getEventByUid`

Get an event with given UID.
