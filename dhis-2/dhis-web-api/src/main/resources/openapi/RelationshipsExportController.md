# Get Relationships

## Specific endpoints

### `getRelationships`

Get relationships matching the given query parameters.

Exactly one parameter of `trackedEntity` (`tei`), `enrollment` or `event` has to be specified.

### `getRelationshipByUid`

Get a relationship with the given UID.

### `getRelationshipByUid.parameter.uid`

Get a relationship with the given UID.

### `getRelationshipByUid.parameter.fields`

Get only the specified fields in the JSON response. This query parameter allows you to remove
unnecessary fields from
the response and in some cases decrease the response time. Refer to
https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata.html#webapi_metadata_field_filter
for how to use it.

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

### `*.parameter.RelationshipRequestParams.order`

`<propertyName1:sortDirection>[,<propertyName2:sortDirection>...]`

Get relationships in given order. Relationships can be ordered by the following case-sensitive
properties

* `createdAt`

Valid `sortDirection`s are `asc` and `desc`. `sortDirection` is case-insensitive. `sortDirection`
defaults to `asc` for properties without explicit `sortDirection` as in `order=createdAt`.

Relationships are ordered by newest (internal id desc) by default meaning when no `order` parameter
is provided.

### `*.parameter.RelationshipRequestParams.fields`

Get only the specified fields in the JSON response. This query parameter allows you to remove
unnecessary fields from
the JSON response and in some cases decrease the response time. Refer to
https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata.html#webapi_metadata_field_filter
for how to use it.
