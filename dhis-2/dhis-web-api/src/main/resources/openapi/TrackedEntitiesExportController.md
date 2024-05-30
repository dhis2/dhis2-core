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

Get only the specified fields in the JSON response. This query parameter allows you to remove
unnecessary fields from the response and in some cases decrease the response time. Refer to
https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata.html#webapi_metadata_field_filter
for how to use it.

NOTE: this query parameter has no effect on a response in CSV!

### `getAttributeValueFile`

Get a tracked entity attribute value file or image for given tracked entity and tracked entity
attribute UID. Images are returned in their original dimension.

### `getAttributeValueFile.parameter.program`

The program to be used for evaluating the users access to the file content. A program is required
when requesting a program-specific tracked entity attribute. When no program is specified, access
to the file content is evaluated based on the users access to the relevant tracked entity type.

### `getAttributeValueImage`

Get an event data value image for given event and data element UID. Images are returned in their
original dimension by default. This endpoint is only supported for data elements of value type
image.

### `getAttributeValueImage.parameter.program`

The program to be used for evaluating the users access to the image. A program is required when
requesting a program-specific tracked entity attribute. When no program is specified, access to the
image is evaluated based on the users access to the relevant tracked entity type.

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

Get tracked entities owned by given `orgUnit`.

### `*.parameter.TrackedEntityRequestParams.orgUnit`

**DEPRECATED as of 2.41:** Use parameter `orgUnits` instead where UIDs have to be separated by
comma!

`<orgUnit1-uid>[;<orgUnit2-uid>...]`

Get tracked entities owned by given `orgUnit`.

### `*.parameter.TrackedEntityRequestParams.orgUnitMode`

Get tracked entities using given organisation unit mode.

### `*.parameter.TrackedEntityRequestParams.ouMode`

**DEPRECATED as of 2.41:** Use parameter `orgUnitMode` instead.

Get tracked entities using given organisation unit mode.

### `*.parameter.TrackedEntityRequestParams.program`

### `*.parameter.TrackedEntityRequestParams.enrollmentStatus`

Get tracked entities with an enrollment in the given status.

### `*.parameter.TrackedEntityRequestParams.programStatus`

Get tracked entities with an enrollment in the given status.

**DEPRECATED as of 2.42:** Use parameter `enrollmentStatus` instead.

See `enrollmentStatus` for details.

### `*.parameter.TrackedEntityRequestParams.followUp`

### `*.parameter.TrackedEntityRequestParams.updatedAfter`

Get tracked entities updated after given date and time.
This parameter is inclusive, so results with the exact date and time specified will be included in the response.

### `*.parameter.TrackedEntityRequestParams.updatedBefore`

Get tracked entities updated before given date and time.
This parameter is inclusive, so results with the exact date and time specified will be included in the response.

### `*.parameter.TrackedEntityRequestParams.updatedWithin`

Get tracked entities updated since given ISO-8601 duration.

### `*.parameter.TrackedEntityRequestParams.enrollmentEnrolledAfter`

Get tracked entities with enrollments that were enrolled after given date and time.
This parameter is inclusive, so results with the exact date and time specified will be included in the response.

### `*.parameter.TrackedEntityRequestParams.enrollmentEnrolledBefore`

Get tracked entities with enrollments that were enrolled before given date and time.
This parameter is inclusive, so results with the exact date and time specified will be included in the response.

### `*.parameter.TrackedEntityRequestParams.enrollmentOccurredAfter`

Get tracked entities with enrollments occurred after given date and time.
This parameter is inclusive, so results with the exact date and time specified will be included in the response.

### `*.parameter.TrackedEntityRequestParams.enrollmentOccurredBefore`

Get tracked entities with enrollments occurred before given date and time.
This parameter is inclusive, so results with the exact date and time specified will be included in the response.

### `*.parameter.TrackedEntityRequestParams.trackedEntityType`

### `*.parameter.TrackedEntityRequestParams.trackedEntities`

`<trackedEntity1-uid>[,<trackedEntity2-uid>...]`

Get tracked entities with given UID(s).

### `*.parameter.TrackedEntityRequestParams.trackedEntity`

**DEPRECATED as of 2.41:** Use parameter `trackedEntities` instead where UIDs have to be separated
by comma!

`<trackedEntity1-uid>[;<trackedEntity2-uid>...]`

Get tracked entities with given UID(s).

### `*.parameter.TrackedEntityRequestParams.assignedUserMode`

### `*.parameter.TrackedEntityRequestParams.assignedUsers`

`<user1-uid>[,<user2-uid>...]`

Get tracked entities with an event assigned to given user(s). Specifying `assignedUsers` is only
valid if `assignedUserMode` is either `PROVIDED` or not specified.

### `*.parameter.TrackedEntityRequestParams.assignedUser`

**DEPRECATED as of 2.41:** Use parameter `assignedUsers` instead where UIDs have to be separated by
comma!

`<user1-uid>[;<user2-uid>...]`

Get tracked entities with an event assigned to given user(s). Specifying `assignedUsers` is only
valid if `assignedUserMode` is either `PROVIDED` or not specified.

### `*.parameter.TrackedEntityRequestParams.programStage`

### `*.parameter.TrackedEntityRequestParams.eventStatus`

Get tracked entities with an event with the given status. `eventStatus` must be specified together
with `eventOccurredAfter` and `eventOccurredBefore`.

### `*.parameter.TrackedEntityRequestParams.eventOccurredAfter`

Get tracked entities with an event occurred after given date and time.
This parameter is inclusive, so results with the exact date and time specified will be included in the response.
`eventOccurredAfter` must be specified together with `eventStatus` and `eventOccurredBefore`.

### `*.parameter.TrackedEntityRequestParams.eventOccurredBefore`

Get tracked entities with an event occurred before given date and time.
This parameter is inclusive, so results with the exact date and time specified will be included in the response.
`eventOccurredBefore` must be specified together with `eventStatus` and `eventOccurredAfter`.

### `*.parameter.TrackedEntityRequestParams.includeDeleted`

### `*.parameter.TrackedEntityRequestParams.potentialDuplicate`

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

NOTE: this query parameter has no effect on a CSV response!

### `*.parameter.TrackedEntityRequestParams.query`

**REMOVED as of 2.41:** Use parameter `filter`!

### `*.parameter.TrackedEntityRequestParams.attribute`

**REMOVED as of 2.41:** Use parameter `filter`!

### `*.parameter.TrackedEntityRequestParams.includeAllAttributes`

**REMOVED as of 2.41:**!

### `*.parameter.TrackedEntityRequestParams.filter`

`<filter1>[,<filter2>...]`

Get tracked entities matching given filters on attributes. A filter is a colon separated attribute
UID with optional operator and value pairs. Example: `filter=H9IlTX2X6SL:sw:A` with operator starts
with `sw` followed by a value. Special characters like `+` need to be percent-encoded so `%2B`
instead of `+`. Characters such as `:` (colon) or `,` (comma), as part of the filter value, need to
be escaped by `/` (slash). Likewise, `/` needs to be escaped. Multiple operator/value pairs for the
same attribute as `filter=AuPLng5hLbE:gt:438901703:lt:448901704` are allowed. Repeating the same
attribute UID is not allowed. A user needs metadata read access to the attribute and data read
access to the program (if the program is without registration) or the program stage (if the program
is with registration).

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
