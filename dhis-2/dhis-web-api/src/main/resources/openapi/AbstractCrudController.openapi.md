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
Apply filter operations to the returned list of metadata. 
See [Metadata-object-filter](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata.html#webapi_metadata_object_filter).

### Parameter: `fields`
Limit the response to specific field(s). 
See [Metadata-field-filter](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata.html#webapi_metadata_field_filter).

### Parameter: `order`
Enum: `<property>:asc`, `<property>:desc`,`<property>:iasc`, `<property>:idesc`
Order the output using a specified order, only properties that are both persisted and simple (no collections, idObjects etc) are supported. iasc and idesc are case insensitive sorting.

### Parameter: `WebOptions.paging`
Indicates whether to return lists of elements in pages.

### Parameter: `WebOptions.page`
Defines which page number to return.

### Parameter: `WebOptions.pageSize`
Defines the number of elements to return for each page.

### Parameter: `WebOptions.rootJunction`
Combine filters with `AND` (default) or `OR`

### Parameter: `ImportOptions.preheatCache`
Turn cache-map preheating on/off. This is on by default, turning this off will make initial load time for importer much shorter (but will make the import itself slower). This is mostly used for cases where you have a small XML/JSON file you want to import, and don't want to wait for cache-map preheating.

### Parameter: `ImportOptions.mergeMode`
Strategy for merging of objects when doing updates. REPLACE will just overwrite the property with the new value provided, MERGE will only set the property if it is not null (only if the property was provided).

### Parameter: `ImportOptions.importStrategy`
Import strategy to use, see below for more information.

### Parameter: `MetadataImportParams.importMode`
Sets overall import mode, decides whether or not to only `VALIDATE` or also `COMMIT` the metadata, this has similar functionality as our old dryRun flag.

### Parameter: `MetadataImportParams.identifier`
Sets the identifier scheme to use for reference matching. `AUTO` means try `UID` first, then `CODE`.

### Parameter: `MetadataImportParams.importReportMode`
Sets the `ImportReport` mode, controls how much is reported back after the import is done. `ERRORS` only includes ObjectReports for object which has errors. `FULL` returns an ObjectReport for all objects imported, and `DEBUG` returns the same plus a name for the object (if available).

### Parameter: `MetadataImportParams.preheatMode`
Sets the preheater mode, used to signal if preheating should be done for `ALL` (as it was before with `preheatCache=true`) or do a more intelligent scan of the objects to see what to preheat (now the default), setting this to `NONE` is not recommended.

### Parameter: `MetadataImportParams.importStrategy`
Sets import strategy, `CREATE_AND_UPDATE` will try and match on identifier, if it doesn't exist, it will create the object.

### Parameter: `MetadataImportParams.atomicMode`
Sets atomic mode, in the old importer we always did a best effort import, which means that even if some references did not exist, we would still import (i.e. missing data elements on a data element group import). Default for new importer is to not allow this, and similar reject any validation errors. Setting the `NONE` mode emulated the old behavior.

### Parameter: `MetadataImportParams.mergeMode`
Sets the merge mode, when doing updates we have two ways of merging the old object with the new one, `MERGE` mode will only overwrite the old property if the new one is not-null, for `REPLACE` mode all properties are overwritten regardless of null or not. (*)

### Parameter: `MetadataImportParams.flushMode`
Sets the flush mode, which controls when to flush the internal cache. It is strongly recommended to keep this to `AUTO` (which is the default). Only use `OBJECT` for debugging purposes, where you are seeing hibernate exceptions and want to pinpoint the exact place where the stack happens (hibernate will only throw when flushing, so it can be hard to know which object had issues).

### Parameter: `MetadataImportParams.skipSharing`
Skip sharing properties, does not merge sharing when doing updates, and does not add user group access when creating new objects.

### Parameter: `MetadataImportParams.skipValidation`
Skip validation for import. **NOT RECOMMENDED**

### Parameter: `MetadataImportParams.async`
Asynchronous import, returns immediately with a _Location_ header pointing to the location of the _importReport_. The payload also contains a json object of the job created.

### Parameter: `MetadataImportParams.inclusionStrategy`
`NON_NULL` includes properties which are not null, `ALWAYS` include all properties, `NON_EMPTY` includes non-empty properties (will not include strings of 0 length, collections of size 0, etc.)

### Parameter: `MetadataImportParams.userOverrideMode`
Allows you to override the user property of every object you are importing, the options are `NONE` (do nothing), `CURRENT` (use import user), `SELECTED` (select a specific user using overrideUser=X)

### Parameter: `MetadataImportParams.overrideUser`
If userOverrideMode is `SELECTED`, use this parameter to select the user you want override with.

### Parameter: `GistParams.absoluteUrls`
Use absolute (`true`) or relative URLs (`false`, default) when linking to other objects.
See [Gist absoluteUrls parameter](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata-gist.html#gist_parameters_absoluteUrls).

### Parameter: `GistParams.auto`
The extent of fields to include when no specific list of fields is provided using `fields` so that  that listed fields are automatically determined.
See [Gist auto parameter](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata-gist.html#the-auto-parameter).

### Parameter: `GistParams.describe`
When `true` the query is not executed but the planned execution is described back similar to using _describe_ in SQL/database context.

### Parameter: `GistParams.fields`
A comma seperated list of fields to include in the response. `*` includes all `auto` detected fields.
See [Gist fields parameter](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata-gist.html#gist_parameters_fields).

### Parameter: `GistParams.filter`
A comma seperated list of filters.
See [Gist filter parameter](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata-gist.html#gist_parameters_filter).


### Parameter: `GistParams.headless`
Endpoints returning a list by default wrap the items with an envelope containing the pager and the list, which is named according to the type of object listed.
See [Gist headless parameter](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata-gist.html#gist_parameters_headless).

### Parameter: `GistParams.inverse`
Inverse can be used in context of a collection field gist of the form /api/<object-type>/<object-id>/<field-name>/gist to not list all items that are contained in the member collection but all items that are not contained in the member collection.
See [Gist inverse parameter](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata-gist.html#the-inverse-parameter).

### Parameter: `GistParams.locale`
Switch translation language of display names. If not specified the translation language is the one configured in the users account settings.
See [Gist locale parameter](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata-gist.html#gist_parameters_locale).

### Parameter: `GistParams.order`
To sort the list of items - one or more order expressions can be given.
See [Gist order parameter](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata-gist.html#gist_parameters_order).

### Parameter: `GistParams.page`
The viewed page in paged list starting with 1 for the first page
See [Gist page parameter](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata-gist.html#gist_parameters_page).

### Parameter: `GistParams.pageSize`
The number of items on a page. Maximum is 1000 items.
See [Gist pageSize parameter](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata-gist.html#gist_parameters_pageSize).

### Parameter: `GistParams.references`
By default, the Gist API includes links to referenced objects. This can be disabled by using `references=false`.

### Parameter: `GistParams.rootJunction`
Combine `filter`s with `AND` (default) or `OR` logic combinator
See [Gist rootJunction parameter](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata-gist.html#gist_parameters_rootJunction).

### Parameter: `GistParams.total`
By default, a gist query will not count the total number of matches should those exceed the `pageSize` limit.
Using `total=true` the pager includes the total number of matches.
See [Gist total parameter](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata-gist.html#gist_parameters_total).

### Parameter: `GistParams.translate`
Fields like _name_ or _shortName_ can be translated (internationalised).
By default, any translatable field that has a translation is returned translated given that the user requesting the gist has an interface language configured.
To return the plain non-translated field use `translate=false`.
See [Gist translate parameter](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata-gist.html#gist_parameters_translate).