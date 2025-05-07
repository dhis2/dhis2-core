# Get Relationships

## Specific endpoints

### `getRelationships`

Get relationships matching the specified query parameters.

Exactly one parameter of `trackedEntity`, `enrollment` or `event` has to be specified.

### `getRelationshipByUid`

Get a relationship with the specified UID.

### `getRelationshipByUid.parameter.uid`

Get a relationship with the specified UID.

### `getRelationshipByUid.parameter.fields`

Get only the specified fields in the JSON response. This query parameter allows you to remove
unnecessary fields from the response and in some cases decrease the response time. Refer to
https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata.html#webapi_metadata_field_filter
for how to use it.

## Common for all endpoints

### `*.parameter.RelationshipRequestParams.trackedEntity`

Get relationships where the specified tracked entity is either the from or to side of the 
relationship.

### `*.parameter.RelationshipRequestParams.enrollment`

Get relationships where the specified enrollment is either the from or to side of the relationship.

### `*.parameter.RelationshipRequestParams.event`

Get relationships where the specified event is either the from or to side of the relationship. 

### `*.parameter.RelationshipRequestParams.order`

`<propertyName1:sortDirection>[,<propertyName2:sortDirection>...]`

Get relationships in specified order. Relationships can be ordered by the following case-sensitive
properties:

* `createdAt`
* `createdAtClient`

Valid `sortDirection`s are `asc` and `desc`. `sortDirection` is case-insensitive. `sortDirection`
defaults to `asc` for properties without explicit `sortDirection` as in `order=createdAt`.

Relationships are ordered by newest (internal id desc) by default, when no `order` parameter
is provided.

### `*.parameter.RelationshipRequestParams.fields`

Get only the specified fields in the JSON response. This query parameter allows you to remove
unnecessary fields from the JSON response and in some cases decrease the response time. Refer to
https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata.html#webapi_metadata_field_filter
for how to use it.

### `*.parameter.RelationshipRequestParams.includeDeleted`

Get soft-deleted relationships by specifying `includeDeleted=true`. Soft-deleted relationships 
are excluded by default. This parameter allows you to retrieve relationships that have been 
marked as deleted but are still in the database.
