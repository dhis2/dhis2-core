# Datastore Descriptions

## Common for all endpoints

### `*.parameter.namespace`
Name of the namespace.

### `*.parameter.key`
Name of the key.

## Specific endpoints

### `*.response.404`
In case no entry in the namespace is found for the key.

### `getNamespaces`
Lists names of available namespaces.

### `getKeysInNamespace`
List all keys in a specific namespace.

### `getEntries`
List all entries in a namespace matching the search criteria.

### `deleteNamespace`
Delete a namespace including all its entries.

### `getKeyJsonValue`
Get JSON value of a single entry.

### `getKeyJsonValueMetaData`
Get metadata for a single entry.

### `addKeyJsonValue`
Add a key-value pair to a namespace.

### `updateKeyJsonValue`
Update a value for a specific key and namespace.

### `deleteKeyJsonValue`
Delete a single key from a specific namespace.

