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

### `*.parameter.EventRequestParams.fields`

Get only the specified fields in the JSON response. This query parameter allows you to remove unnecessary fields from
the JSON response and in some cases decrease the response time. Refer to
https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata.html#webapi_metadata_field_filter
for how to use it.

NOTE: this query parameter has no effect on a CSV response!

### `*.parameter.EventRequestParams.filter`

`<filter1>[,<filter2>...]`

Get events matching given filters on data values. A filter is a colon separated data element UID with operator and value
pairs. Example: `filter=H9IlTX2X6SL:sw:A` with operator starts with `sw` followed by a value. Special characters
like `+` need to be percent-encoded so `%2B` instead of `+`. Multiple operator/value pairs for the same data element
like `filter=AuPLng5hLbE:gt:438901703:lt:448901704` are allowed. Repeating the same data element UID is not allowed.
Operator and values are case-insensitive. A user needs metadata read access to the data element and data read access to
the program (if the program is without registration) or the program stage (if the program is with registration).

Valid operators are:

- `EQ` - equal to
- `IEQ` - equal to
- `GE` - greater than or equal to
- `GT` - greater than
- `LE` - less than or equal to
- `LT` - less than
- `NE` - not equal to
- `NEQ` - not equal to
- `NIEQ` - not equal to
- `IN` - equal to one of the multiple values separated by semicolon ";"
- `ILIKE` - is like (case-insensitive)
- `LIKE` - like (free text match)
- `NILIKE` - not like
- `NLIKE` - not like
- `SW` - starts with
- `EW` - ends with

### `*.parameter.EventRequestParams.filterAttributes`

`<filter1>[,<filter2>...]`

Get events matching given filters on tracked entity attributes. A filter is a colon separated attribute UID with
optional operator and value pairs. Example: `filter=H9IlTX2X6SL:sw:A` with operator starts with `sw` followed by a
value. Special characters like `+` need to be percent-encoded so `%2B` instead of `+`. Multiple operator/value pairs for
the same attribute like `filter=AuPLng5hLbE:gt:438901703:lt:448901704` are allowed. Repeating the same attribute UID is
not allowed. Operator and values are case-insensitive. A user needs metadata read access to the attribute and data
read access to the program (if the program is without registration) or to the program stage (if the program is with
registration).

Valid operators are:

- `EQ` - equal to
- `IEQ` - equal to
- `GE` - greater than or equal to
- `GT` - greater than
- `LE` - less than or equal to
- `LT` - less than
- `NE` - not equal to
- `NEQ` - not equal to
- `NIEQ` - not equal to
- `IN` - equal to one of the multiple values separated by semicolon ";"
- `ILIKE` - is like (case-insensitive)
- `LIKE` - like (free text match)
- `NILIKE` - not like
- `NLIKE` - not like
- `SW` - starts with
- `EW` - ends with
