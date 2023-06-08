# Get Tracked Entities

## `getTrackedEntities`

Get tracked entities matching given query parameters.

## `getTrackedEntityByUid`

Get a tracked entity with given UID.

### `getTrackedEntityByUid.parameter.program`

Get tracked entities enrolled in the specified `program`.
When a `program` is specified, all the Tracked Entity attributes and the Program specific ones are included.

## Common for all endpoints

### `*.parameter.TrackedEntityRequestParams.query`

### `*.parameter.TrackedEntityRequestParams.attribute`

### `*.parameter.TrackedEntityRequestParams.orgUnits`

`<orgUnit1-uid>[,<orgUnit2-uid>...]`

Get tracked entities owned by given `orgUnit`.

### `*.parameter.TrackedEntityRequestParams.orgUnit`

**DEPRECATED as of 2.41:** Use parameter `orgUnits` instead where UIDs have to be separated by comma!

`<orgUnit1-uid>[;<orgUnit2-uid>...]`

Get tracked entities owned by given `orgUnit`.

### `*.parameter.TrackedEntityRequestParams.ouMode`

Get events using given organisation unit selection mode.

### `*.parameter.TrackedEntityRequestParams.program`

### `*.parameter.TrackedEntityRequestParams.programStatus`

### `*.parameter.TrackedEntityRequestParams.followUp`

### `*.parameter.TrackedEntityRequestParams.updatedAfter`

### `*.parameter.TrackedEntityRequestParams.updatedBefore`

### `*.parameter.TrackedEntityRequestParams.updatedWithin`

### `*.parameter.TrackedEntityRequestParams.enrollmentEnrolledAfter`

### `*.parameter.TrackedEntityRequestParams.enrollmentEnrolledBefore`

### `*.parameter.TrackedEntityRequestParams.enrollmentOccurredAfter`

### `*.parameter.TrackedEntityRequestParams.enrollmentOccurredBefore`

### `*.parameter.TrackedEntityRequestParams.trackedEntityType`

### `*.parameter.TrackedEntityRequestParams.trackedEntities`

`<trackedEntity1-uid>[,<trackedEntity2-uid>...]`

Get tracked entities with given UID(s).

### `*.parameter.TrackedEntityRequestParams.trackedEntity`

**DEPRECATED as of 2.41:** Use parameter `trackedEntities` instead where UIDs have to be separated by comma!

`<trackedEntity1-uid>[;<trackedEntity2-uid>...]`

Get tracked entities with given UID(s).

### `*.parameter.TrackedEntityRequestParams.assignedUserMode`

### `*.parameter.TrackedEntityRequestParams.assignedUsers`

`<user1-uid>[,<user2-uid>...]`

Get tracked entities with an event assigned to given user(s). Specifying `assignedUsers` is only valid
if `assignedUserMode` is either `PROVIDED` or not specified.

### `*.parameter.TrackedEntityRequestParams.assignedUser`

**DEPRECATED as of 2.41:** Use parameter `assignedUsers` instead where UIDs have to be separated by comma!

`<user1-uid>[;<user2-uid>...]`

Get tracked entities with an event assigned to given user(s). Specifying `assignedUsers` is only valid
if `assignedUserMode` is either `PROVIDED` or not specified.

### `*.parameter.TrackedEntityRequestParams.programStage`

### `*.parameter.TrackedEntityRequestParams.eventStatus`

### `*.parameter.TrackedEntityRequestParams.eventOccurredAfter`

### `*.parameter.TrackedEntityRequestParams.eventOccurredBefore`

### `*.parameter.TrackedEntityRequestParams.skipMeta`

### `*.parameter.TrackedEntityRequestParams.includeDeleted`

### `*.parameter.TrackedEntityRequestParams.includeAllAttributes`

Include in the response all the attributes linked to a Tracked Entity. By default, only the Tracked
Entity attributes are returned, not the Program specific ones.
If a `program` is specified, `includeAllAttributes` is ignored and all the Tracked Entity attributes 
and the Program specific ones are included.

### `*.parameter.TrackedEntityRequestParams.potentialDuplicate`

