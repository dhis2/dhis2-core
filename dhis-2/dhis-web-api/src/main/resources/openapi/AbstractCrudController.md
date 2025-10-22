# Defaults for Controller Descriptions

## Specific endpoints

### `getObjectList`

List all {entityType}s

### `getObjectList.response.200`
The _list_ of {entityType}s

### `getObjectList.parameter.fields`
`<field-name>[,<field-name>...]` 

### `getObject`

View a {entityType}

### `deleteObject`

Deletes the {entityType} provided by ID.

### `deleteObject.response.200`
A message envelop containing the report detailing the deletion outcome.

## Common for all endpoints

### `*.parameter.filter`
Apply filter operations to the returned list of metadata. 
See [Metadata-object-filter](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata.html#webapi_metadata_object_filter).

### `*.parameter.fields`
Limit the response to specific field(s). 
See [Metadata-field-filter](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata.html#webapi_metadata_field_filter).

### `*.parameter.order`
Enum: `<property>:asc`, `<property>:desc`,`<property>:iasc`, `<property>:idesc`
Order the output using a specified order, only properties that are both persisted and simple (no collections, idObjects etc) are supported. iasc and idesc are case insensitive sorting.

### `*.parameter.WebOptions.paging`
Indicates whether to return lists of elements in pages.

### `*.parameter.WebOptions.page`
Defines which page number to return.

### `*.parameter.WebOptions.pageSize`
Defines the number of elements to return for each page.

### `*.parameter.WebOptions.rootJunction`
Combine filters with `AND` (default) or `OR`

### `*.parameter.ImportOptions.preheatCache`
Turn cache-map preheating on/off. This is on by default, turning this off will make initial load time for importer much shorter (but will make the import itself slower). This is mostly used for cases where you have a small XML/JSON file you want to import, and don't want to wait for cache-map preheating.

### `*.parameter.ImportOptions.mergeMode`
Strategy for merging of objects when doing updates. REPLACE will just overwrite the property with the new value provided, MERGE will only set the property if it is not null (only if the property was provided).

### `*.parameter.ImportOptions.importStrategy`
Import strategy to use, see below for more information.

### `*.parameter.MetadataImportParams.importMode`
Sets overall import mode, decides whether or not to only `VALIDATE` or also `COMMIT` the metadata, this has similar functionality as our old dryRun flag.

### `*.parameter.MetadataImportParams.identifier`
Sets the identifier scheme to use for reference matching. `AUTO` means try `UID` first, then `CODE`.

### `*.parameter.MetadataImportParams.importReportMode`
Sets the `ImportReport` mode, controls how much is reported back after the import is done. `ERRORS` only includes ObjectReports for object which has errors. `FULL` returns an ObjectReport for all objects imported, and `DEBUG` returns the same plus a name for the object (if available).

### `*.parameter.MetadataImportParams.preheatMode`
Sets the preheater mode, used to signal if preheating should be done for `ALL` (as it was before with `preheatCache=true`) or do a more intelligent scan of the objects to see what to preheat (now the default), setting this to `NONE` is not recommended.

### `*.parameter.MetadataImportParams.importStrategy`
Sets import strategy, `CREATE_AND_UPDATE` will try and match on identifier, if it doesn't exist, it will create the object.

### `*.parameter.MetadataImportParams.atomicMode`
Sets atomic mode, in the old importer we always did a best effort import, which means that even if some references did not exist, we would still import (i.e. missing data elements on a data element group import). Default for new importer is to not allow this, and similar reject any validation errors. Setting the `NONE` mode emulated the old behavior.

### `*.parameter.MetadataImportParams.mergeMode`
Sets the merge mode, when doing updates we have two ways of merging the old object with the new one, `MERGE` mode will only overwrite the old property if the new one is not-null, for `REPLACE` mode all properties are overwritten regardless of null or not. (*)

### `*.parameter.MetadataImportParams.flushMode`
Sets the flush mode, which controls when to flush the internal cache. It is strongly recommended to keep this to `AUTO` (which is the default). Only use `OBJECT` for debugging purposes, where you are seeing hibernate exceptions and want to pinpoint the exact place where the stack happens (hibernate will only throw when flushing, so it can be hard to know which object had issues).

### `*.parameter.MetadataImportParams.skipSharing`
Skip sharing properties, does not merge sharing when doing updates, and does not add user group access when creating new objects.

### `*.parameter.MetadataImportParams.skipValidation`
Skip validation for import. **NOT RECOMMENDED**

### `*.parameter.MetadataImportParams.async`
Asynchronous import, returns immediately with a _Location_ header pointing to the location of the _importReport_. The payload also contains a json object of the job created.

### `*.parameter.MetadataImportParams.inclusionStrategy`
`NON_NULL` includes properties which are not null, `ALWAYS` include all properties, `NON_EMPTY` includes non-empty properties (will not include strings of 0 length, collections of size 0, etc.)
