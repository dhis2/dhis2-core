# Get Enrollments

## `getEnrollments`

Get enrollments matching given query parameters.

### `getEnrollments.parameter.enrolledAfter`

Get enrollments enrolled after given date.

### `getEnrollments.parameter.enrolledBefore`

Get enrollments enrolled before given date.

### `getEnrollments.parameter.enrollment`

`<enrollment1-uid>[;<enrollment2-uid>...]`

Get enrollments with given UID(s).

### `getEnrollments.parameter.followUp`

Get enrollments with given follow-up status of the instance for the given program.

### `getEnrollments.parameter.includeDeleted`

Get soft-deleted enrollments by specifying `includeDeleted=true`. Soft-deleted enrollments are excluded by default.

### `getEnrollments.parameter.orgUnit`

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

### `getEnrollments.parameter.page`

Get given number of pages. Defaults to `1`.

### `getEnrollments.parameter.pageSize`

Get given number of entries per page. Defaults to `50`.

### `getEnrollments.parameter.totalPages`

Get the total number of pages by specifying `totalPages=true`. Defaults to `false`.

### `getEnrollments.parameter.skipPaging`

Get all enrollments by specifying `skipPaging=true`. Defaults to `false`, meaning that by default requests are
paginated.

**Be aware that the performance is directly related to the amount of data requested. Larger pages will take more time to
return.**

### `getEnrollments.parameter.order`

`<propertyName1:sortDirection>[,<propertyName2:sortDirection>...]`

Get entries in given order. Valid `sortDirection`s are `asc` and `desc`. `propName` is case-sensitive, `sortDirection`
is case-insensitive.

## `getEnrollmentByUid`

Get an enrollment with given UID.
