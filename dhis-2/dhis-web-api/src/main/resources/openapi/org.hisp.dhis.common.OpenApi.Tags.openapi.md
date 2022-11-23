# Global Tags Descriptions
This configuration is used to add descriptions to `@Tags` values.
These are included as a `tags` array in the root of an OpenAPI configuration.
Tag usages in the endpoints only refer to these description details by the tag name.

## Tag `synthetic`
Marks Endpoints that have been merged from more than one method.
This means there is no 1:1 relation between the REST endpoint described by OpenAPI
and the Java controller methods that implement them. 

## Tag `metadata`
Endpoint is part of the metadata API

## Tag `user`
Endpoint is part of the user API

## Tag `scheduling`
Endpoint is part of the scheduling API

### `externalDocs`
https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/scheduling.html

Job scheduling in DHIS2