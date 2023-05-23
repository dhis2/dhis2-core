# Get Tracked Entities

## `getTrackedEntities`

Get tracked entities matching given query parameters.

### `getTrackedEntities.parameter.query`

### `getTrackedEntities.parameter.attribute`

### `getTrackedEntities.parameter.orgUnits`

`<orgUnit1-uid>[,<orgUnit2-uid>...]`

Get tracked entities owned by given `orgUnit`.

### `getTrackedEntities.parameter.orgUnit`

**DEPRECATED as of 2.41:** Use parameter `orgUnits` instead where UIDs have to be separated by comma!

`<orgUnit1-uid>[;<orgUnit2-uid>...]`

Get tracked entities owned by given `orgUnit`.

### `getTrackedEntities.parameter.ouMode`

Get events using given organisation unit selection mode.

### `getTrackedEntities.parameter.program`

### `getTrackedEntities.parameter.programStatus`

### `getTrackedEntities.parameter.followUp`

### `getTrackedEntities.parameter.updatedAfter`

### `getTrackedEntities.parameter.updatedBefore`

### `getTrackedEntities.parameter.updatedWithin`

### `getTrackedEntities.parameter.enrollmentEnrolledAfter`

### `getTrackedEntities.parameter.enrollmentEnrolledBefore`

### `getTrackedEntities.parameter.enrollmentOccurredAfter`

### `getTrackedEntities.parameter.enrollmentOccurredBefore`

### `getTrackedEntities.parameter.trackedEntityType`

### `getTrackedEntities.parameter.trackedEntities`

`<trackedEntity1-uid>[,<trackedEntity2-uid>...]`

Get tracked entities with given UID(s).

### `getTrackedEntities.parameter.trackedEntity`

**DEPRECATED as of 2.41:** Use parameter `trackedEntities` instead where UIDs have to be separated by comma!

`<trackedEntity1-uid>[;<trackedEntity2-uid>...]`

Get tracked entities with given UID(s).

### `getTrackedEntities.parameter.assignedUserMode`

### `getTrackedEntities.parameter.assignedUsers`

`<user1-uid>[,<user2-uid>...]`

Get tracked entities with an event assigned to given user(s). Specifying `assignedUsers` is only valid
if `assignedUserMode` is either `PROVIDED` or not specified.

### `getTrackedEntities.parameter.assignedUser`

**DEPRECATED as of 2.41:** Use parameter `assignedUsers` instead where UIDs have to be separated by comma!

`<user1-uid>[;<user2-uid>...]`

Get tracked entities with an event assigned to given user(s). Specifying `assignedUsers` is only valid
if `assignedUserMode` is either `PROVIDED` or not specified.

### `getTrackedEntities.parameter.programStage`

### `getTrackedEntities.parameter.eventStatus`

### `getTrackedEntities.parameter.eventOccurredAfter`

### `getTrackedEntities.parameter.eventOccurredBefore`

### `getTrackedEntities.parameter.skipMeta`

### `getTrackedEntities.parameter.includeDeleted`

### `getTrackedEntities.parameter.includeAllAttributes`

### `getTrackedEntities.parameter.potentialDuplicate`

## `getTrackedEntityByUid`

Get a tracked entity with given UID.

### `getTrackedEntityByUid.parameter.program`
