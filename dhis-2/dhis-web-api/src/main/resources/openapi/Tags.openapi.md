# Global Tags Descriptions
This configuration is used to add descriptions to `@Tags` values.
These are included as a `tags` array in the root of an OpenAPI configuration.
Tag usages in the endpoints only refer to these description details by the tag name.

## Tag `synthetic`
Marks Endpoints that have been merged from more than one method.
This means there is no 1:1 relation between the REST endpoint described by OpenAPI
and the Java controller methods that implement them. 

## Tag `metadata`
Groups endpoints about metadata.

## Tag `user`
Groups endpoints about users.

## Tag `analytics`
Groups endpoints that are analytics related.

## Tag `data`
Groups endpoints about data values.

## Tag `messaging`
Groups endpoints about messages such as email and SMS.

## Tag `system`
Groups endpoints about general system administration.

## Tag `tracker`
Groups endpoints that are tracker related.

### `externalDocs`
https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/tracker.html

## Tag `ui`
Groups endpoints that are UI related.

## Tag `management`
Groups endpoints used to manage system objects.

## Tag `login`
Groups endpoints that are user login related.

## Tag `query`
Groups endpoints used to query information about system object or system state.

## Tag `scheduling`
Endpoint is part of the scheduling API

### `externalDocs`
https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/scheduling.html

Job scheduling in DHIS2