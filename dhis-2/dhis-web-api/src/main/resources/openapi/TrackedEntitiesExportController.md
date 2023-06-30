# Get Tracked Entities

## Specific endpoints

### `getTrackedEntities`

Get tracked entities matching given query parameters.

### `getTrackedEntityByUid`

Get a tracked entity with given UID.

### `getTrackedEntityByUid.parameter.uid`

Get a tracked entity with given UID.

### `getTrackedEntityByUid.parameter.program`

### `getTrackedEntityByUid.parameter.fields`

Get only the specified fields in the JSON response. This query parameter allows you to remove unnecessary fields from
the response and in some cases decrease the response time. Refer to
https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata.html#webapi_metadata_field_filter
for how to use it.

NOTE: this query parameter has no effect on a response in CSV!

## Common for all endpoints

### `*.parameter.TrackedEntityRequestParams.query`

### `*.parameter.TrackedEntityRequestParams.attribute`

### `*.parameter.TrackedEntityRequestParams.orgUnits`

`<orgUnit1-uid>[,<orgUnit2-uid>...]`

Get tracked entities owned by given `orgUnit`.

### `*.parameter.TrackedEntityRequestParams.orgUnit`

**DEPRECATED as of 2.41:** Use parameter `orgUnits` instead where UIDs have to be separated by comma!

`<orgUnit1-uid>[;<orgUnit2-uid>...]`

Get tracked entities owned by given `orgUnit`.

### `*.parameter.TrackedEntityRequestParams.ouMode`

Get events using given organisation unit selection mode.

### `*.parameter.TrackedEntityRequestParams.program`

### `*.parameter.TrackedEntityRequestParams.programStatus`

### `*.parameter.TrackedEntityRequestParams.followUp`

### `*.parameter.TrackedEntityRequestParams.updatedAfter`

### `*.parameter.TrackedEntityRequestParams.updatedBefore`

### `*.parameter.TrackedEntityRequestParams.updatedWithin`

### `*.parameter.TrackedEntityRequestParams.enrollmentEnrolledAfter`

### `*.parameter.TrackedEntityRequestParams.enrollmentEnrolledBefore`

### `*.parameter.TrackedEntityRequestParams.enrollmentOccurredAfter`

### `*.parameter.TrackedEntityRequestParams.enrollmentOccurredBefore`

### `*.parameter.TrackedEntityRequestParams.trackedEntityType`

### `*.parameter.TrackedEntityRequestParams.trackedEntities`

`<trackedEntity1-uid>[,<trackedEntity2-uid>...]`

Get tracked entities with given UID(s).

### `*.parameter.TrackedEntityRequestParams.trackedEntity`

**DEPRECATED as of 2.41:** Use parameter `trackedEntities` instead where UIDs have to be separated by comma!

`<trackedEntity1-uid>[;<trackedEntity2-uid>...]`

Get tracked entities with given UID(s).

### `*.parameter.TrackedEntityRequestParams.assignedUserMode`

### `*.parameter.TrackedEntityRequestParams.assignedUsers`

`<user1-uid>[,<user2-uid>...]`

Get tracked entities with an event assigned to given user(s). Specifying `assignedUsers` is only valid
if `assignedUserMode` is either `PROVIDED` or not specified.

### `*.parameter.TrackedEntityRequestParams.assignedUser`

**DEPRECATED as of 2.41:** Use parameter `assignedUsers` instead where UIDs have to be separated by comma!

`<user1-uid>[;<user2-uid>...]`

Get tracked entities with an event assigned to given user(s). Specifying `assignedUsers` is only valid
if `assignedUserMode` is either `PROVIDED` or not specified.

### `*.parameter.TrackedEntityRequestParams.programStage`

### `*.parameter.TrackedEntityRequestParams.eventStatus`

### `*.parameter.TrackedEntityRequestParams.eventOccurredAfter`

### `*.parameter.TrackedEntityRequestParams.eventOccurredBefore`

### `*.parameter.TrackedEntityRequestParams.skipMeta`

### `*.parameter.TrackedEntityRequestParams.includeDeleted`

### `*.parameter.TrackedEntityRequestParams.includeAllAttributes`

### `*.parameter.TrackedEntityRequestParams.potentialDuplicate`

### `*.parameter.TrackedEntityRequestParams.fields`

Get only the specified fields in the JSON response. This query parameter allows you to remove unnecessary fields from
the JSON response and in some cases decrease the response time. Refer to
https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata.html#webapi_metadata_field_filter
for how to use it.

NOTE: this query parameter has no effect on a CSV response!

### `*.parameter.TrackedEntityRequestParams.filter`

`<filter1>[,<filter2>...]`

Get tracked entities matching given filters on attributes. A filter is a colon separated attribute UID with operator and
value pairs. Example: `filter=H9IlTX2X6SL:sw:A` with operator starts with `sw` followed by a value. Special characters
like `+` need to be percent-encoded so `%2B` instead of `+`. Multiple operator/value pairs for the same attribute
like `filter=AuPLng5hLbE:gt:438901703:lt:448901704` are allowed. Repeating the same attribute UID is not allowed. A user
needs metadata read access to the attribute and data read access to the program (if the program is without registration)
or the program stage (if the program is with registration).

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
