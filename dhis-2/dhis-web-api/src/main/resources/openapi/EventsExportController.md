# Get Events

## Specific endpoints

### `getEvents`

Get events matching given query parameters.

### `getEventByUid`

Get an event with given UID.

### `getEventByUid.parameter.uid`

Get an event with given UID.

### `getEventByUid.parameter.fields`

Get only the specified fields in the JSON response. This query parameter allows you to remove
unnecessary fields from the response and in some cases decrease the response time. Refer to
https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata.html#webapi_metadata_field_filter
for how to use it.

NOTE: this query parameter has no effect on a response in CSV!

### `getEventDataValueFile`

Get an event data value file or image for given event and data element UID. Images are returned in
their original dimension.

### `getEventDataValueImage`

Get an event data value image for given event and data element UID. Images are returned in their
original dimension by default. This endpoint is only supported for data elements of value type
image.

### `getEventChangeLogsByUid`

Get the change logs of all data elements related to that particular event UID.

## Common for all endpoints

### `*.parameter.EventRequestParams.program`

### `*.parameter.EventRequestParams.programStage`

### `*.parameter.EventRequestParams.enrollmentStatus`

Get events from an enrollment in the given status.

### `*.parameter.EventRequestParams.programStatus`

Get events from an enrollment in the given status.

**DEPRECATED as of 2.42:** Use parameter `enrollmentStatus` instead.

See `enrollmentStatus` for details.

### `*.parameter.EventRequestParams.followUp`

Get events with given follow-up status of the instance for the given program.

### `*.parameter.EventRequestParams.trackedEntity`

Get events of tracked entity with given UID.

### `*.parameter.EventRequestParams.orgUnit`

Get events owned by given `orgUnit`.

### `*.parameter.EventRequestParams.orgUnitMode`

Get events using given organisation unit mode.

### `*.parameter.EventRequestParams.assignedUserMode`

Get events assigned to users according to the specified user mode. By default,
all events will be retrieved, regardless of whether a user is assigned.

### `*.parameter.EventRequestParams.assignedUsers`

`<user1-uid>[,<user2-uid>...]`

Get events that are assigned to the given user(s). Specifying `assignedUsers` is only valid
if `assignedUserMode` is either `PROVIDED` or not specified.

### `*.parameter.EventRequestParams.occurredAfter`

Get events that occurred after given date and time.
This parameter is inclusive, so results with the exact date and time specified will be included in the response.

### `*.parameter.EventRequestParams.occurredBefore`

Get events that occurred before given date and time.
This parameter is inclusive, so results with the exact date and time specified will be included in the response.

### `*.parameter.EventRequestParams.scheduledAfter`

Get events that are scheduled after given date and time.
This parameter is inclusive, so results with the exact date and time specified will be included in the response.

### `*.parameter.EventRequestParams.scheduledBefore`

Get events that are scheduled before given date and time.
This parameter is inclusive, so results with the exact date and time specified will be included in the response.

### `*.parameter.EventRequestParams.updatedAfter`

Get events updated after given date and time.
This parameter is inclusive, so results with the exact date and time specified will be included in the response.

### `*.parameter.EventRequestParams.updatedWithin`

Get events updated since given ISO-8601 duration.

### `*.parameter.EventRequestParams.enrollmentEnrolledAfter`

Get events with enrollments that were enrolled after given date and time.
This parameter is inclusive, so results with the exact date and time specified will be included in the response.

### `*.parameter.EventRequestParams.enrollmentEnrolledBefore`

Get events with enrollments that were enrolled before given date and time.
This parameter is inclusive, so results with the exact date and time specified will be included in the response.

### `*.parameter.EventRequestParams.status`

### `*.parameter.EventRequestParams.attributeCategoryCombo`

### `*.parameter.EventRequestParams.attributeCategoryOptions`

`<attributeCategoryOption1-uid>[,<attributeCategoryOption2-uid>...]`

### `*.parameter.EventRequestParams.includeDeleted`

Get soft-deleted events by specifying `includeDeleted=true`. Soft-deleted events are excluded by
default.

### `*.parameter.EventRequestParams.events`

`<event1-uid>[,<event2-uid>...]`

Get events with given UID(s).

### `*.parameter.EventRequestParams.order`

`<propertyName1:sortDirection>[,<propertyName2:sortDirection>...]`

Get events in given order. Events can be ordered by data elements and tracked entity attributes by
passing a UID instead of a property name. This will order events by the values of the specified
attribute and data element not their UIDs. Events can also be ordered by the following
case-sensitive properties

* `assignedUser`
* `assignedUserDisplayName`
* `attributeOptionCombo`
* `completedAt`
* `completedBy`
* `createdAt`
* `createdAtClient`
* `createdBy`
* `deleted`
* `enrolledAt`
* `enrollment`
* `enrollmentStatus`
* `event`
* `followUp`
* `occurredAt`
* `orgUnit`
* `program`
* `programStage`
* `scheduledAt`
* `status`
* `storedBy`
* `trackedEntity`
* `updatedAt`
* `updatedAtClient`
* `updatedBy`

Valid `sortDirection`s are `asc` and `desc`. `sortDirection` is case-insensitive. `sortDirection`
defaults to `asc` for properties or UIDs without explicit `sortDirection` as in `order=scheduledAt`.

Events are ordered by newest (internal id desc) by default meaning when no `order` parameter is
provided.

### `*.parameter.EventRequestParams.fields`

Get only the specified fields in the JSON response. This query parameter allows you to remove
unnecessary fields from the JSON response and in some cases decrease the response time. Refer to
https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata.html#webapi_metadata_field_filter
for how to use it.

NOTE: this query parameter has no effect on a CSV response!

### `*.parameter.EventRequestParams.filter`

`<filter1>[,<filter2>...]`

Get events matching the given filters on data values. A filter is a colon separated data element UID
with optional operator and value pairs. 

We differentiate between two types of operators: unary and 
binary. Unary operators don't require a value, while binary operators do.
- Unary: `filter=H9IlTX2X6SL:null`
- Binary: `filter=H9IlTX2X6SL:sw:A` 

Special characters like `+` must be percent-encoded (`%2B` instead of `+`). Characters like `:` and
`,` in filter values must be escaped with `/`. Likewise, `/` needs to be escaped.
Multiple operators are allowed for the same data element, e.g.,
`filter=AuPLng5hLbE:gt:438901703:lt:448901704`. Operators and values are
case-insensitive. A user needs metadata read access to the data element and data read access to the
program (if the program is without registration) or the program stage (if the program is with
registration).

All of the following operators are supported regardless of the value type. Values are compared using
text comparison unless stated otherwise. Integer and decimal value types are treated as Postgres
integer and numeric data types for the specified operators.

Valid binary operators are:
- `eq` - equal to (uses integer/numeric semantics for integer/decimal value types)
- `ieq` - equal to
- `ge` - greater than or equal to (uses integer/number semantics for integer/decimal value types)
- `gt` - greater than (uses integer/number semantics for integer/decimal value types)
- `le` - less than or equal to (uses integer/number semantics for integer/decimal value types)
- `lt` - less than (uses integer/number semantics for integer/decimal value types)
- `ne` - not equal to (uses integer/number semantics for integer/decimal value types)
- `neq` - not equal to (uses integer/number semantics for integer/decimal value types)
- `nieq` - not equal to
- `in` - equal to one of the multiple values separated by semicolon ";" (uses integer/number semantics for integer/decimal value types)
- `ilike` - is like
- `like` - like (free text match)
- `nilike` - not like
- `nlike` - not like
- `sw` - starts with
- `ew` - ends with

Right now all matches are case-insensitive so for example `eq` and `ieq` (`i` for `insensitive`)
behave in the same way.

Valid unary operators are:
- `null` - has no value
- `!null` - has a value

### `*.parameter.EventRequestParams.filterAttributes`

`<filter1>[,<filter2>...]`

Get events matching the given filters on tracked entity attributes. A filter is a colon separated 
attribute UID with optional operator and value pairs. 

We differentiate between two types of
operators: unary and binary. Unary operators don't require a value, while binary operators do.
- Unary: `filterAttributes=H9IlTX2X6SL:null`
- Binary: `filterAttributes=H9IlTX2X6SL:sw:A` 

Special characters like `+` must be percent-encoded (`%2B` instead of `+`). Characters like `:` and 
`,` in filter values must be escaped with `/`. Likewise, `/` needs to be escaped.
Multiple operators are allowed for the same attribute, e.g.,
`filter=AuPLng5hLbE:gt:438901703:lt:448901704`. Operators and values are
case-insensitive. A user needs metadata read access to the attribute and data read access to the
program (if the program is without registration) or the program stage (if the program is with
registration).

All of the following operators are supported regardless of the value type. Values are compared using
text comparison unless stated otherwise. Integer and decimal value types are treated as Postgres
integer and numeric data types for the specified operators.

Valid binary operators are:
- `eq` - equal to (uses integer/numeric semantics for integer/decimal value types)
- `ieq` - equal to
- `ge` - greater than or equal to (uses integer/number semantics for integer/decimal value types)
- `gt` - greater than (uses integer/number semantics for integer/decimal value types)
- `le` - less than or equal to (uses integer/number semantics for integer/decimal value types)
- `lt` - less than (uses integer/number semantics for integer/decimal value types)
- `ne` - not equal to (uses integer/number semantics for integer/decimal value types)
- `neq` - not equal to (uses integer/number semantics for integer/decimal value types)
- `nieq` - not equal to
- `in` - equal to one of the multiple values separated by semicolon ";" (uses integer/number semantics for integer/decimal value types)
- `ilike` - is like
- `like` - like (free text match)
- `nilike` - not like
- `nlike` - not like
- `sw` - starts with
- `ew` - ends with

Right now all matches are case-insensitive so for example `eq` and `ieq` (`i` for `insensitive`)
behave in the same way.

Valid unary operators are:
- `null` - has no value
- `!null` - has a value
