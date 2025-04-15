# Get Tracked Entities

## Specific endpoints

### `getTrackedEntities`

Get tracked entities matching given query parameters.

### `getTrackedEntityByUid`

Get a tracked entity with a given UID.

### `getTrackedEntityByUid.parameter.uid`

Get a tracked entity with a given UID.

### `getTrackedEntityByUid.parameter.program`

Get tracked entity with tracked entity attribute and enrollment data from the specified tracker
program. The given `program` ownership will be used to determine access to the tracked
entity.

### `getTrackedEntityByUid.parameter.fields`

Get only the specified fields in the JSON response. This query parameter allows you to remove
unnecessary fields from the response and sometimes decrease the response time. Refer to
https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata.html#webapi_metadata_field_filter
for how to use it.

NOTE: This query parameter does not affect a response in CSV!

### `getAttributeValueFile`

Get a tracked entity attribute value file or image for a given tracked entity and tracked entity
attribute UID. Images are returned in their original dimension.

### `getAttributeValueFile.parameter.program`

The program to be used for evaluating the user's access to the file content. A program is required
when requesting a program-specific tracked entity attribute. When no program is specified, access
to the file content is evaluated based on the user's access to the relevant tracked entity type.

### `getAttributeValueImage`

Get an event data value image for a given event and data element UID. Images are returned in their
original dimension by default. This endpoint is only supported for data elements of value-type
image.

### `getAttributeValueImage.parameter.program`

The program to be used for evaluating the user's access to the image. A program is required when
requesting a program-specific tracked entity attribute. When no program is specified, access to the
image is evaluated based on the user's access to the relevant tracked entity type.

### `getTrackedEntityAttributeChangeLog`

Get the change logs of all tracked entity attributes related to that particular tracked entity UID.
It will return change logs of tracked entity attributes within the tracked entity type.

### `getTrackedEntityAttributeChangeLog.parameter.program`

Get the change logs of all tracked entity attributes related to that particular tracked entity and
program UID. It will return change logs of tracked entity attributes within the tracked entity type
and program attributes too.

## Common for all endpoints

### `*.parameter.TrackedEntityRequestParams.orgUnits`

`<orgUnit1-uid>[,<orgUnit2-uid>...]`

Get tracked entities owned by given orgUnits relative to the `orgUnitMode` and
`program` parameters. If a `program` is provided, the ownership is determined with this program. When
no program is provided, the registration orgUnit for the tracked entity would be used to determine
ownership.

- When `orgUnitMode=SELECTED` - or no `orgUnitMode` is given (default) - the tracked entities
  owned by the `orgUnits` are returned.
- When `orgUnitMode=CHILDREN` the tracked entities owned by the orgUnits or by the orgUnits
  direct children is returned.
- When `orgUnitMode=DESCENDANTS` the tracked entities owned by the orgUnits or any of its
  descendants are returned.
- When `orgUnitMode=ALL`, `orgUnitMode=CAPTURE` or `orgUnitMode=ACCESSIBLE` the `orgUnits` parameter
  is not allowed.

### `*.parameter.TrackedEntityRequestParams.orgUnitMode`

Get tracked entities using the given `orgUnitMode` and `program` parameters. If a
`program` is provided, the ownership is determined with this program. When no program is provided,
the registration organisation unit for the tracked entity would be used to determine ownership.

- When `orgUnitMode=SELECTED`, `orgUnitMode=CHILDREN` or `orgUnitMode=DESCENDANTS`, the `orgUnit`
  parameter is required to specify which tracked entities to return.
- When `orgUnitMode=ALL` tracked entities will be downloaded irrespective of the organization
  unit they are owned by. To use this parameter, the user needs the `Search Tracked
  entity in all org units` authority.
- When `orgUnitMode=ACCESSIBLE` tracked entities owned by any organisation unit in the
  user's capture scope will be returned.
- When `orgUnitMode=CAPTURE` tracked entities that have an enrollment organisation unit
  in the user's capture scope will be returned.

### `*.parameter.TrackedEntityRequestParams.program`

Get tracked entities with tracked entity attributes and enrollment data from the specified tracker
program. The ownership of the given `program` will be used to determine access to the tracked
entities. Only tracked entities with an enrollment into the `program` will be returned. Required 
if neither `trackedEntityType` nor `trackedEntities` are specified.

### `*.parameter.TrackedEntityRequestParams.enrollmentStatus`

Get tracked entities that have at least one enrollment with the specified status.

### `*.parameter.TrackedEntityRequestParams.programStatus`

Get tracked entities that have at least one enrollment with the status specified.

**DEPRECATED as of 2.42:** Use parameter `enrollmentStatus` instead.

### `*.parameter.TrackedEntityRequestParams.followUp`

Get tracked entities that have at least one enrollment that is marked with follow-up.

### `*.parameter.TrackedEntityRequestParams.updatedAfter`

Get tracked entities that were updated after the given time. A tracked entity is considered
updated if any of its tracked entity attributes, enrollments, events or relationships have been
modified. The `updatedAt` property of the tracked entity reflects the latest time at which any
of its tracked entity attributes, enrollments, events or relationships were modified. This
parameter is inclusive, so results with the exact date and time specified will be included
in the response.

### `*.parameter.TrackedEntityRequestParams.updatedBefore`

Get tracked entities that were updated before the given time. A tracked entity is considered
updated if any of its tracked entity attributes, enrollments, events or relationships have been
modified. The `updatedAt` property of the tracked entity reflects the latest time at which any
of its tracked entity attributes, enrollments, events or relationships were modified. This
parameter is inclusive, so results with the exact date and time specified will be included in
the response.

### `*.parameter.TrackedEntityRequestParams.updatedWithin`

Get tracked entities updated since the given ISO-8601 duration. A tracked entity is considered
updated if any of its tracked entity attributes, enrollments, events or relationships have been
modified. The `updatedAt` property of the tracked entity reflects the latest time at which any
of its tracked entity attributes, enrollments, events or relationships were modified.

### `*.parameter.TrackedEntityRequestParams.enrollmentEnrolledAfter`

Get tracked entities that have at least one enrollment with an enrollment date after this date.
This parameter is inclusive, so results with the exact date and time specified will be included
in the response.

### `*.parameter.TrackedEntityRequestParams.enrollmentEnrolledBefore`

Get tracked entities that have at least one enrollment with an enrollment date before this date.
This parameter is inclusive, so results with the exact date and time specified will be included
in the response.

### `*.parameter.TrackedEntityRequestParams.enrollmentOccurredAfter`

Get tracked entities that have at least one enrollment with an occurred date after this date.
This parameter is inclusive, so results with the exact date and time specified will be included
in the response.

### `*.parameter.TrackedEntityRequestParams.enrollmentOccurredBefore`

Get tracked entities that have at least one enrollment with an occurred date before this date.
This parameter is inclusive, so results with the exact date and time specified will be included
in the response.

### `*.parameter.TrackedEntityRequestParams.trackedEntityType`

Get tracked entities with the given tracked entity type. Required if neither `program` nor 
`trackedEntities` are specified.

### `*.parameter.TrackedEntityRequestParams.trackedEntities`

`<trackedEntity1-uid>[,<trackedEntity2-uid>...]`

Get tracked entities with given UID(s).

### `*.parameter.TrackedEntityRequestParams.assignedUserMode`

Get tracked entities with events assigned to users according to the specified user mode. By default,
all events will be retrieved, regardless of whether a user is assigned.

- When `assignedUserMode=ALL` or no `assignedUserMode`(default) is given, tracked entities are
  returned irrespective of whether they contain events assigned to a user or not.

- When `assignedUserMode=CURRENT`, tracked entities that have at least one event
  assigned to the logged-in user will be returned.

- When `assignedUserMode=ANY`,tracked entities that have at least one event with
  an assigned user will be returned.

- When `assignedUserMode=NONE`, tracked entities that have no event
  that are assigned to any user will be returned.

- When `assignedUserMode=PROVIDED`, tracked
  entities that have any events assigned to the users specified in `assignedUsers` will be
  returned. `assignedUsers` is required for this mode.

### `*.parameter.TrackedEntityRequestParams.assignedUsers`

`<user1-uid>[,<user2-uid>...]`

Get tracked entities with an event assigned to given user(s). Specifying `assignedUsers` is only
valid if `assignedUserMode` is either `PROVIDED` or not specified.

### `*.parameter.TrackedEntityRequestParams.programStage`

Define which program stage the `eventStatus`, `eventOccurredBefore` and `eventOccurredAfter`
parameters should be applied to.

### `*.parameter.TrackedEntityRequestParams.eventStatus`

Get tracked entities that have at least one event with the given status. `eventStatus` must be
specified together with `eventOccurredAfter` and `eventOccurredBefore`.
with `eventOccurredAfter` and `eventOccurredBefore`.

Only return tracked entities that have at least one event in the specified status. See also
`programStage`.

### `*.parameter.TrackedEntityRequestParams.eventOccurredAfter`

Get tracked entities with an event that occurred after the given date and time. This parameter
is inclusive, so results with the exact date and time specified will be included in the response.
`eventOccurredAfter` must be specified together with `eventStatus` and `eventOccurredBefore`.

### `*.parameter.TrackedEntityRequestParams.eventOccurredBefore`

Get tracked entities with an event occurred before given date and time. This parameter is
inclusive, so results with the exact date and time specified will be included
in the response. `eventOccurredBefore` must be specified together with `eventStatus` and
`eventOccurredAfter`.

### `*.parameter.TrackedEntityRequestParams.includeDeleted`

Include deleted tracked entities, enrollments, events and relationships in the response.

### `*.parameter.TrackedEntityRequestParams.potentialDuplicate`

Get tracked entities that are marked as potential duplicates.

### `*.parameter.TrackedEntityRequestParams.order`

`<propertyName1:sortDirection>[,<propertyName2:sortDirection>...]`

Get tracked entities in given order. Tracked entities can be ordered by tracked entity attributes by
passing a UID instead of a property name. This will order tracked entities by the values of the
specified attribute not their UIDs. Tracked entities can also be ordered by the following
case-sensitive properties

* `createdAt`
* `createdAtClient`
* `enrolledAt`
* `inactive`
* `trackedEntity`
* `updatedAt`
* `updatedAtClient`

Valid `sortDirection`s are `asc` and `desc`. `sortDirection` is case-insensitive. `sortDirection`
defaults to `asc` for properties or UIDs without explicit `sortDirection` as in `order=createdAt`.

Tracked entities are ordered by newest (internal id desc) by default meaning when no `order`
parameter is provided.

### `*.parameter.TrackedEntityRequestParams.fields`

Get only the specified fields in the JSON response. This query parameter allows you to remove
unnecessary fields from the JSON response and in some cases decrease the response time. Refer to
https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata.html#webapi_metadata_field_filter
for how to use it.

NOTE: This query parameter has no effect on a CSV response!

### `*.parameter.TrackedEntityRequestParams.query`

**REMOVED as of 2.41:** Use parameter `filter`!

### `*.parameter.TrackedEntityRequestParams.attribute`

**REMOVED as of 2.41:** Use parameter `filter`!

### `*.parameter.TrackedEntityRequestParams.includeAllAttributes`

**REMOVED as of 2.41:**!

### `*.parameter.TrackedEntityRequestParams.filter`

`<filter1>[,<filter2>...]`

Get tracked entities matching the given filters attributes. A filter is a colon separated
attribute UID with optional operator and value pairs. 

We differentiate between two types of
operators: unary and binary. Unary operators don't require a value, while binary operators do.
- Unary: `filter=H9IlTX2X6SL:null`
- Binary: `filter=H9IlTX2X6SL:sw:A`

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
