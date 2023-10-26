# Get Enrollments

## Specific endpoints

### `getEnrollments`

Get enrollments matching given query parameters.

### `getEnrollmentByUid`

Get an enrollment with given UID.

### `getEnrollmentByUid.parameter.uid`

Get an enrollment with given UID.

### `getEnrollmentByUid.parameter.fields`

Get only the specified fields in the JSON response. This query parameter allows you to remove
unnecessary fields from
the response and in some cases decrease the response time. Refer to
https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata.html#webapi_metadata_field_filter
for how to use it.

## Common for all endpoints

### `*.parameter.EnrollmentRequestParams.enrolledAfter`

Get enrollments with an enrollment date after the given date.

### `*.parameter.EnrollmentRequestParams.enrolledBefore`

Get enrollments with an enrollment date before the given date.

### `*.parameter.EnrollmentRequestParams.enrollments`

`<enrollment1-uid>[,<enrollment2-uid>...]`

Get enrollments with given UID(s).

### `*.parameter.EnrollmentRequestParams.enrollment`

**DEPRECATED as of 2.41:** Use parameter `enrollments` instead where UIDs have to be separated by
comma!

See `enrollments` for details.

### `*.parameter.EnrollmentRequestParams.followUp`

Get enrollments with the given follow-up status.

### `*.parameter.EnrollmentRequestParams.includeDeleted`

Get soft-deleted enrollments by specifying `includeDeleted=true`. Soft-deleted enrollments are
excluded by default.

### `*.parameter.EnrollmentRequestParams.orgUnits`

`<orgUnit1-uid>[,<orgUnit2-uid>...]`

Get enrollments owned by given `orgUnits` relative to the `orgUnitMode`.

- When `orgUnitMode=SELECTED` - or no `orgUnitMode` is given (default) - the enrollments owned by
  the `orgUnits` are returned.
- When `orgUnitMode=CHILDREN` the enrollments owned by the `orgUnits` or by the `orgUnits` direct
  children is returned.
- When `orgUnitMode=DESCENDANTS` the enrollments owned by the `orgUnits` or any of its descendants
  are returned.
- When `orgUnitMode=ALL`, `orgUnitMode=CAPTURE` or `orgUnitMode=ACCESSIBLE` the `orgUnits` parameter
  is not allowed.

### `*.parameter.EnrollmentRequestParams.orgUnit`

**DEPRECATED as of 2.41:** Use parameter `orgUnits` instead where UIDs have to be separated by
comma!

See `orgUnits` for details.

### `*.parameter.EnrollmentRequestParams.orgUnitMode`

Get enrollments using given organisation unit selection mode.

- When `orgUnitMode=SELECTED`, `orgUnitMode=CHILDREN` or `orgUnitMode=DESCENDANTS`, the `orgUnit`
  parameter is
  required to specify which enrollments to return.
- When `orgUnitMode=ALL` enrollments will be downloaded irrespective of the organization unit they
  are
  owned by. To use this parameter, the user needs the `Search Tracked entity in all org units`
  authority.
- When `orgUnitMode=ACCESSIBLE` enrollments owned by any org unit in the users capture scope will be
  returned.
- When `orgUnitMode=CAPTURE` enrollments that has an enrollment org unit in the users capture scope
  will
  be returned.

### `*.parameter.EnrollmentRequestParams.ouMode`

**DEPRECATED as of 2.41:** Use parameter `orgUnitMode` instead.

See `orgUnitMode` for details.

### `*.parameter.EnrollmentRequestParams.program`

Get enrollments enrolled in the given program.

### `*.parameter.EnrollmentRequestParams.programStatus`

Get enrollments enrolled in a program with the given status.

### `*.parameter.EnrollmentRequestParams.trackedEntityType`

Get enrollments of tracked entities of the given type.

### `*.parameter.EnrollmentRequestParams.trackedEntity`

Get enrollments of tracked entity with the given UID.

### `*.parameter.EnrollmentRequestParams.updatedAfter`

Get enrollments updated after the given date.

### `*.parameter.EnrollmentRequestParams.updatedWithin`

Get enrollments updated within the given ISO-8601 duration.

### `*.parameter.EnrollmentRequestParams.order`

`<propertyName1:sortDirection>[,<propertyName2:sortDirection>...]`

Get enrollments in given order. Enrollments can be ordered by the following case-sensitive
properties

* `completedAt`
* `createdAt`
* `createdAtClient`
* `enrolledAt`
* `updatedAt`
* `updatedAtClient`

Valid `sortDirection`s are `asc` and `desc`. `sortDirection` is case-insensitive. `sortDirection`
defaults to `asc` for properties without explicit `sortDirection` as in `order=enrolledAt`.

Enrollments are ordered by newest (internal id desc) by default meaning when no `order` parameter is
provided.

### `*.parameter.EnrollmentRequestParams.fields`

Get only the specified fields in the JSON response. This query parameter allows you to remove
unnecessary fields from
the JSON response and in some cases decrease the response time. Refer to
https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata.html#webapi_metadata_field_filter
for how to use it.
