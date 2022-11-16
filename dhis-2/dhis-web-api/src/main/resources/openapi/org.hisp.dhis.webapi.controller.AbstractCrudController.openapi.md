# Defaults for Controller Descriptions

## Endpoint `getObjectList`

List all {entityType}s

### Response: `200`
The _list_ of {entityType}s

### Parameter: `fields`
`<field-name>[,<field-name>...]` 

## Endpoint `getObject`

View a {entityType}

## Endpoint `deleteObject`

Deletes the {entityType} provided by ID.

### Response: `200`
A message envelop containing the report detailing the deletion outcome.

## Any Endpoint `*`

### Parameter: `filter`
Format: 

* unary: `<field>:<operator>`
* binary: `<field>:<operator>:<value>`


### Parameter: `fields`
e.g. 
* `<field-name>` 
* `<object>[<field-name>, ...]`
* `*, <object>[*]`