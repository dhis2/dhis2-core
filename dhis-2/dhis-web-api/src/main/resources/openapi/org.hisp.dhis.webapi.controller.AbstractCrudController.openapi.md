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

### Parameter: `WebOptions.paging`
Set `true` (default) to enable paging, `false` to disable paging of results

### Parameter: `WebOptions.page`
The current page of the result list

### Parameter: `WebOptions.pageSize`
The size of the result list

### Parameter: `WebOptions.viewClass`
Good question...

### Parameter: `WebOptions.rootJunction`
Combine filters with `AND` (default) or `OR`

### Parameter: `WebOptions.manage`
Good question...

### Parameter: `WebOptions.assumeTrue`
Good question...

### Parameter: `WebOptions.lastUpdated`
Good question...