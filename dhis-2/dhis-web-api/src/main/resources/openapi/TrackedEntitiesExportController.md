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
when requesting a program-specific tracked entity attribute. When no program is specified, access to
the file content is evaluated based on the users access to the relevant tracked entity type.

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

Get tracked entities and enrollments owned by given orgUnits relative to the `orgUnitMode` and
`program` parameters. If a `program` is provided, the ownership is determied with this program. When
no program is provided, the registration orgUnit for the tracked entity would be used to determine
ownership.

- When `orgUnitMode=SELECTED` - or no `orgUnitMode` is given (default) - the tracked entity or
  enrollments owned by the `orgUnits` are returned.
- When `orgUnitMode=CHILDREN` the tracked entity or enrollments owned by the orgUnits or by the
  orgUnits direct children is returned.
- When `orgUnitMode=DESCENDANTS` the tracked entity or enrollments owned by the orgUnits or any of
  its descendants are returned.
- When `orgUnitMode=ALL`, `orgUnitMode=CAPTURE` or `orgUnitMode=ACCESSIBLE` the `orgUnits` parameter
  is not allowed.

### `*.parameter.TrackedEntityRequestParams.orgUnit`

**DEPRECATED as of 2.41:** Use parameter `orgUnits` instead where UIDs have to be separated by
comma!

`<orgUnit1-uid>[;<orgUnit2-uid>...]`

Get tracked entities owned by given `orgUnit`.

### `*.parameter.TrackedEntityRequestParams.orgUnitMode`

Get tracked entities and enrollments using given `orgUnitMode` and `program` parameters. If a
`program` is provided, the ownership is determied with this program. When no program is provided,
the registration organisation unit for the tracked entity would be used to determine ownership.

- When `orgUnitMode=SELECTED`, `orgUnitMode=CHILDREN` or `orgUnitMode=DESCENDANTS`, the `orgUnit`
  parameter is required to specify which tracked entity or enrollments to return.
- When `orgUnitMode=ALL` tracked entity or enrollments will be downloaded irrespective of the
  organization unit they are owned by. To use this parameter, the user needs the `Search Tracked
  entity in all org units` authority.
- When `orgUnitMode=ACCESSIBLE` tracked entity or enrollments owned by any organisation unit in the users
  capture scope will be returned.
- When `orgUnitMode=CAPTURE` tracked entity or enrollments that has an enrollment organisation unit in the
  users capture scope will be returned.

### `*.parameter.TrackedEntityRequestParams.ouMode`

**DEPRECATED as of 2.41:** Use parameter `orgUnitMode` instead.

Get tracked entities using given organisation unit mode.

### `*.parameter.TrackedEntityRequestParams.program`

Get tracked entity with tracked entity attribute and enrollment data from the specified program. The
ownership of the given `program` will be used to determine access to the tracked entities.

### `*.parameter.TrackedEntityRequestParams.programStatus`

Get tracked entities that have at least one enrollment with the status specified.

Valid options are:
- `ACTIVE`
- `COMPLETED`
- `CANCELLED`

### `*.parameter.TrackedEntityRequestParams.followUp`

Get tracked entities that has at least one enrollment that is marked with follow up.

### `*.parameter.TrackedEntityRequestParams.updatedAfter`

Get tracked entities that is updated after the given time. The update can be on the tracked entity
or on one of the tracked entity attributes, enrollments or events for that tracked entity.

### `*.parameter.TrackedEntityRequestParams.updatedBefore`

Get tracked entities that is updated before the given time. The update can be on the tracked entity
or on one of the tracked entity attributes, enrollments or events for that tracked entity.

### `*.parameter.TrackedEntityRequestParams.updatedWithin`

### `*.parameter.TrackedEntityRequestParams.enrollmentEnrolledAfter`

Get tracked entities that has at least one enrollment with an enrollment date after the date
specified in `enrollmentEnrolledAfter`.

### `*.parameter.TrackedEntityRequestParams.enrollmentEnrolledBefore`

Get tracked entities that has at least one enrollment with an enrollment date before the date
specified in `enrollmentEnrolledBefore`.

### `*.parameter.TrackedEntityRequestParams.enrollmentOccurredAfter`

Get tracked entities that has at least one enrollment with an occurred date after the date
specified in `enrollmentOccurredAfter`.

### `*.parameter.TrackedEntityRequestParams.enrollmentOccurredBefore`

Get tracked entities that has at least one enrollment with an occurred date before the date
specified in `enrollmentOccurredBefore`.

### `*.parameter.TrackedEntityRequestParams.trackedEntityType`

Get tracked entities with the given tracked entity type. Required if no `program` is specified.

### `*.parameter.TrackedEntityRequestParams.trackedEntities`

`<trackedEntity1-uid>[,<trackedEntity2-uid>...]`

Get tracked entities with given UID(s).

### `*.parameter.TrackedEntityRequestParams.trackedEntity`

**DEPRECATED as of 2.41:** Use parameter `trackedEntities` instead where UIDs have to be separated
by comma!

`<trackedEntity1-uid>[;<trackedEntity2-uid>...]`

Get tracked entities with given UID(s).

### `*.parameter.TrackedEntityRequestParams.assignedUserMode`

Get tracked entities and enrollments based on the user assignment in the events of these
enrollments.

- When `assignedUserMode=ME` tracked entities and enrollments that has at least one event assigned
  to the logged in user will be returned.
- When `assignedUserMode=ANYONE` tracked entities and enrollments that has at least one event with
  an assigned user will be returned.
- When `assignedUserMode=NONE` tracked entities with no events assigned to any user will be
  returned. 
- When `assignedUserMode=SELECTED` The `assignedUsers` parameter will be required, and the tracked
  entities and enrollments that has any events assigned to the users specified will be returned.

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

### `*.parameter.TrackedEntityRequestParams.eventOccurredAfter`

### `*.parameter.TrackedEntityRequestParams.eventOccurredBefore`

### `*.parameter.TrackedEntityRequestParams.includeDeleted`

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
