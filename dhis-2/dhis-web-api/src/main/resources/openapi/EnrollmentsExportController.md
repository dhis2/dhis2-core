# Get Enrollments

## Specific endpoints

### `getEnrollments`

Get enrollments matching given query parameters.

### `getEnrollmentByUid`

Get an enrollment with given UID.

### `getEnrollmentByUid.parameter.uid`

Get an enrollment with given UID.

### `getEnrollmentByUid.parameter.fields`

Get only the specified fields in the JSON response. This query parameter allows you to remove unnecessary fields from
the response and in some cases decrease the response time. Refer to
https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata.html#webapi_metadata_field_filter
for how to use it.

## Common for all endpoints

### `*.parameter.EnrollmentRequestParams.enrolledAfter`

Get enrollments enrolled after given date.

### `*.parameter.EnrollmentRequestParams.enrolledBefore`

Get enrollments enrolled before given date.

### `*.parameter.EnrollmentRequestParams.enrollments`

`<enrollment1-uid>[,<enrollment2-uid>...]`

Get enrollments with given UID(s).

### `*.parameter.EnrollmentRequestParams.enrollment`

**DEPRECATED as of 2.41:** Use parameter `enrollments` instead where UIDs have to be separated by comma!

`<enrollment1-uid>[;<enrollment2-uid>...]`

Get enrollments with given UID(s).

### `*.parameter.EnrollmentRequestParams.followUp`

Get enrollments with given follow-up status of the instance for the given program.

### `*.parameter.EnrollmentRequestParams.includeDeleted`

Get soft-deleted enrollments by specifying `includeDeleted=true`. Soft-deleted enrollments are excluded by default.

### `*.parameter.EnrollmentRequestParams.orgUnits`

`<orgUnit1-uid>[,<orgUnit2-uid>...]`

Get enrollments owned by given `orgUnit`.

### `*.parameter.EnrollmentRequestParams.orgUnit`

**DEPRECATED as of 2.41:** Use parameter `orgUnits` instead where UIDs have to be separated by comma!

`<orgUnit1-uid>[;<orgUnit2-uid>...]`

Get enrollments owned by given `orgUnit`.

### `*.parameter.EnrollmentRequestParams.ouMode`

Get enrollments using given organisation unit selection mode.

### `*.parameter.EnrollmentRequestParams.program`

Get enrollments enrolled in given program.

### `*.parameter.EnrollmentRequestParams.programStatus`

Get enrollments enrolled in a program with given status.

### `*.parameter.EnrollmentRequestParams.trackedEntityType`

Get enrollments of tracked entities of given type.

### `*.parameter.EnrollmentRequestParams.trackedEntity`

Get enrollments of tracked entity with given UID.

### `*.parameter.EnrollmentRequestParams.updatedAfter`

Get enrollments updated after given date.

### `*.parameter.EnrollmentRequestParams.updatedWithin`

Get enrollments updated since given ISO-8601 duration.

### `*.parameter.EnrollmentRequestParams.fields`

Get only the specified fields in the JSON response. This query parameter allows you to remove unnecessary fields from
the JSON response and in some cases decrease the response time. Refer to
https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata.html#webapi_metadata_field_filter
for how to use it.
