# Get Enrollments

## `getEnrollments`

Get enrollments matching given query parameters.

### `getEnrollments.parameter.enrolledAfter`

Get enrollments with an enrollment date after the given date.

### `getEnrollments.parameter.enrolledBefore`

Get enrollments with an enrollment date before the given date.

### `getEnrollments.parameter.enrollment`

`<enrollment1-uid>[;<enrollment2-uid>...]`

Get enrollments with the given UID(s).

### `getEnrollments.parameter.followUp`

Get enrollments with the given follow-up status.

### `getEnrollments.parameter.includeDeleted`

Get soft-deleted enrollments by specifying `includeDeleted=true`. Soft-deleted enrollments are excluded by default.

### `getEnrollments.parameter.orgUnit`

Get enrollments owned by given `orgUnit` relative to the `ouMode`. 
- When `ouMode=SELECTED` - or no `ouMode` is given - the enrollments owned by the `orgUnit` is returned.
- When `ouMode=CHILDREN` the enrollments owned by the `orgUnit` or by the `orgUnit`s direct children is returned.
- When `ouMode=DESCENDANTS` the enrollments owned by the `orgUnit` or by any of the descendants under the `orgUnit` is returned.
- When `ouMode=ALL`, `ouMode=CAPTURE` or `ouMode=ACCESSIBLE` the `orgUnit` parameter will be ignored.

### `getEnrollments.parameter.ouMode`

Get enrollments using given organisation unit selection mode.
- When `ouMode=SELECTED`, `ouMode=CHILDREN` or `ouMode=DESCENDANTS`, the `orgUnit` parameter is also required to specify which enrollments to download.
- When `ouMode=ALL` enrollments will be downloaded irrespective of the organization unit they are owned by. To use this parameter, the user needs the TODO: FIND AUTHORITY NAME, or ALL authority.
- When `ouMode=ACCESSIBLE` enrollments owned by any org unit in the users capture scope will be returned.
- When `ouMode=CAPTURE` enrollments that has an enrollment org unit in the users capture scope will be returned.

### `getEnrollments.parameter.program`

Get enrollments enrolled in the given program.

### `getEnrollments.parameter.programStatus`

Get enrollments enrolled in a program with the given status.

### `getEnrollments.parameter.trackedEntityType`

Get enrollments of tracked entities of the given type.

### `getEnrollments.parameter.trackedEntity`

Get enrollments of tracked entity with the given UID.

### `getEnrollments.parameter.updatedAfter`

Get enrollments updated after the given date.

### `getEnrollments.parameter.updatedWithin`

Get enrollments updated within the given ISO-8601 duration.

## `getEnrollmentByUid`

Get an enrollment with the given UID.
