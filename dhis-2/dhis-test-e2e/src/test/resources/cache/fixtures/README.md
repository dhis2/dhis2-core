Cache fixtures for ETag cache tests live in this directory.

Use one JSON file per metadata schema plural, for example `users.json` or
`dataApprovalLevels.json`.

The `MetadataEndpointCacheTest` suite will fail with a clear message when a schema needs a curated
fixture because schema-driven object generation is not stable enough for that type.
