# Get Enrollments

## Endpoint `getEnrollments`

Get enrollments matching given query parameters.

### Parameter: `enrolledAfter`

Get enrollments enrolled after given date.

### Parameter: `enrolledBefore`

Get enrollments enrolled before given date.

### Parameter: `enrollment`

`<enrollment1-uid>[;<enrollment2-uid>...]`

Get enrollments with given UID(s).

### Parameter: `followUp`

Get enrollments with given follow-up status of the instance for the given program.

### Parameter: `includeDeleted`

Get soft-deleted enrollments by specifying `includeDeleted=true`. Soft-deleted enrollments are excluded by default.

### Parameter: `orgUnit`

Get enrollments owned by given `orgUnit`.

### Parameter: `ouMode`

Get enrollments using given organisation unit selection mode.

### Parameter: `program`

Get enrollments enrolled in given program.

### Parameter: `programStatus`

Get enrollments enrolled in a program with given status.

### Parameter: `trackedEntityType`

Get enrollments of tracked entities of given type.

### Parameter: `trackedEntity`

Get enrollments of tracked entity with given UID.

### Parameter: `updatedAfter`

Get enrollments updated after given date.

### Parameter: `updatedWithin`

Get enrollments updated since given ISO-8601 duration.

### Parameter: `page`

Get given number of pages. Defaults to `1`.

### Parameter: `pageSize`

Get given number of entries per page. Defaults to `50`.

### Parameter: `totalPages`

Get the total number of pages by specifying `totalPages=true`. Defaults to `false`.

### Parameter: `skipPaging`

Get all enrollments by specifying `skipPaging=true`. Defaults to `false`, meaning that by default requests are
paginated.

**Be aware that the performance is directly related to the amount of data requested. Larger pages will take more time to
return.**

### Parameter: `order`

`<propertyName1:sortDirection>[,<propertyName2:sortDirection>...]`

Get entries in given order. Valid `sortDirection`s are `asc` and `desc`. `propName` is case-sensitive, `sortDirection`
is case-insensitive.

## Endpoint `getEnrollmentByUid`

Get an enrollment with given UID.
