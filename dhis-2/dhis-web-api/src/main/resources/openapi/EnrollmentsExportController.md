# Get Enrollments

## `getEnrollments`

Get enrollments matching given query parameters.

### `getEnrollments.parameter.enrolledAfter`

Get enrollments enrolled after given date.

### `getEnrollments.parameter.enrolledBefore`

Get enrollments enrolled before given date.

### `getEnrollments.parameter.enrollments`

`<enrollment1-uid>[,<enrollment2-uid>...]`

Get enrollments with given UID(s).

### `getEnrollments.parameter.enrollment`

**DEPRECATED as of 2.41:** Use parameter `enrollments` instead where UIDs have to be separated by comma!

`<enrollment1-uid>[;<enrollment2-uid>...]`

Get enrollments with given UID(s).

### `getEnrollments.parameter.followUp`

Get enrollments with given follow-up status of the instance for the given program.

### `getEnrollments.parameter.includeDeleted`

Get soft-deleted enrollments by specifying `includeDeleted=true`. Soft-deleted enrollments are excluded by default.

### `getEnrollments.parameter.orgUnits`

`<orgUnit1-uid>[,<orgUnit2-uid>...]`

Get enrollments owned by given `orgUnit`.

### `getEnrollments.parameter.orgUnit`

**DEPRECATED as of 2.41:** Use parameter `orgUnits` instead where UIDs have to be separated by comma!

`<orgUnit1-uid>[;<orgUnit2-uid>...]`

Get enrollments owned by given `orgUnit`.

### `getEnrollments.parameter.ouMode`

Get enrollments using given organisation unit selection mode.

### `getEnrollments.parameter.program`

Get enrollments enrolled in given program.

### `getEnrollments.parameter.programStatus`

Get enrollments enrolled in a program with given status.

### `getEnrollments.parameter.trackedEntityType`

Get enrollments of tracked entities of given type.

### `getEnrollments.parameter.trackedEntity`

Get enrollments of tracked entity with given UID.

### `getEnrollments.parameter.updatedAfter`

Get enrollments updated after given date.

### `getEnrollments.parameter.updatedWithin`

Get enrollments updated since given ISO-8601 duration.

## `getEnrollmentByUid`

Get an enrollment with given UID.
