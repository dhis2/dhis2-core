# Get Relationships

## `getRelationships`

Get relationships matching the given query parameters.

Exactly one parameter of `trackedEntity` (`tei`), `enrollment` or `event` has to be specified.

## `getRelationshipByUid`

Get a relationship with the given UID.

## Common for all endpoints

### `*.parameter.RelationshipRequestParams.trackedEntity`

Get relationships of the given trackedEntity.

### `*.parameter.RelationshipRequestParams.tei`

**DEPRECATED as of 2.41:** Use parameter `trackedEntity` instead.

Get relationships of the given trackedEntity.

### `*.parameter.RelationshipRequestParams.enrollment`

Get the relationships of the given enrollment.

### `*.parameter.RelationshipRequestParams.event`

Get relationships of the given event.
