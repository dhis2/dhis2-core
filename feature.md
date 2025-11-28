https://github.com/dhis2/dhis2-core/pull/21761 improved the performance of this feature
https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata.html#webapi_metadata_field_filter
for HTTP GET /tracker endpoints. Metadata can benefit from this as well but uses some more advanced
features. Tracker previously used the same field filtering implementation but now uses a faster one.
The goal is to add the features that metadata needs to the faster implementation and then to switch
over to it for metadata as well.

dhis-2/dhis-test-web-api/src/test/java/org/hisp/dhis/webapi/controller/tracker/FieldFilterSerializationTest.java
is a great reference and a kind of integration test that ensures the faster implementation is
backward compatible with the previous one.

Lookup the field filter implementation for the current metadata field filtering to see how the
preset you are implementing is supposed to work.
dhis-2/dhis-services/dhis-service-field-filtering/src/main/java/org/hisp/dhis/fieldfiltering/FieldPathHelper.java
is likely where the logic is see applyDefaults

## HTTP API

DHIS2 is running on localhost:8080 with default credentials. Fill these into the examples below to
test

USER=system
PASSWORD=System123
AUTH=system:System123
PROTOCOL=http
HOST=localhost:8080

## Task

This behaves differently in metadata and tracker filtering. The reason is that the tracker field
filtering does not have this path mapping logic implemented. Its important to note that tracker
objects like events should also not get that logic applied. It must somehow be related to schema
information.

`dataSets.id` is shown for `fields=dataSets` for reference/complex objects like `dataSets` in
metatada. `fields=dataValues` means `fields=dataValues[*]` for tracker. The reason is AFAIK
that tracker does not declare `Schemas` for their types

### Sample requests

```http
### metadata filters all fields but id on complex objects
GET {{PROTOCOL}}://{{AUTH}}@{{HOST}}/api/organisationUnits?
pageSize=1&
fields=dataSets
```

```http
### tracker will return all fields
GET {{PROTOCOL}}://{{AUTH}}@{{HOST}}/api/tracker/events?
program=bMcwwoVnbSR&
occurredAfter=2024-01-01&
occurredBefore=2024-12-31&
fields=dataValues
```

### Algorithm idea

I think what we should do is to add a preprocessing step, maybe to the FieldsParser. This feature is
like presets i.e. it turns a fields value from user input into a different one.

### Testing

Its important we have tests that ensure the order in which we map presets, expand paths and apply any
exclusions is correct. Make sure to add ample tests for this into FieldFilterSerializationTest
