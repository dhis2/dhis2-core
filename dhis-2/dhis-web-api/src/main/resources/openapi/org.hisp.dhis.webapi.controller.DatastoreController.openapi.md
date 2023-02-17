# Datastore Descriptions

## Any endpoint `*`

### Parameter: `namespace`
Name of the namespace.

### Parameter: `key`
Name of the key.

### Response: `404`
In case no entry in the namespace is found for the key.

## Endpoint `getNamespaces`
Lists names of available namespaces.

## Endpoint `getKeysInNamespace`
List all keys in a specific namespace.

## Endpoint `getEntries`
List all entries in a namespace matching the search criteria.

## Endpoint `deleteNamespace`
Delete a namespace including all its entries.

## Endpoint `getKeyJsonValue`
Get JSON value of a single entry.

## Endpoint `getKeyJsonValueMetaData`
Get metadata for a single entry.

## Endpoint `addKeyJsonValue`
Add a key-value pair to a namespace.

## Endpoint `updateKeyJsonValue`
Update a value for a specific key and namespace.

## Endpoint `deleteKeyJsonValue`
Delete a single key from a specific namespace.

