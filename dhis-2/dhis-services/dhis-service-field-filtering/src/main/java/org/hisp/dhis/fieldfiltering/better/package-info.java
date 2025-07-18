/**
 * DHIS2 field filtering system for JSON API responses.
 *
 * <p>User input {@code fields=id,name,group[code]} flows through this pipeline:
 *
 * <pre>
 * HTTP Request ─────────────────────────┐
 * ?fields=id,name,group[code]           │
 *                                       ▼
 * ┌─────────────────┐    ┌───────────────────────────────── ┐
 * │  FieldsConverter│    │        FieldsParser              │
 * │  (Spring Web)   ├───►│ 1. Tokenize: [id][,][name]...    │
 * │                 │    │ 2. Parse: Build tree structure   │
 * │                 │    │ 3. Expand presets (:all, :simple)│
 * └─────────────────┘    │ 4. Validate transformations      │
 *                        └─────────────────┬────────────── ─┘
 *                                          │
 *                                          ▼ Fields object
 * ┌─────────────────────────────────────────────────────────┐
 * │              Jackson Serialization                      │
 * │ ObjectMapper.writer()                                   │
 * │   .withAttribute("fields", fields)                      │
 * │   .writeValueAsString(object)                           │
 * └─────────────────┬───────────────────────────────────────┘
 *                   │
 *                   ▼ For each object property
 * ┌──────────────────────────────────────────────────────────┐
 * │           FieldsPropertyFilter                           │
 * │ 1. Test field inclusion: fields.test("name")             │
 * │ 2. Get/set child filters: fields.getChildren("group")    │
 * │ 3. Apply transformations (rename, size, etc.)            │
 * │ 4. Stream or double-serialize when given transformations │
 * └─────────────────┬────────────────────────────────────────┘
 *                   │
 *                   ▼
 * Filtered JSON: {"id":"123","name":"Test","group":{"code":"ABC"}}
 * </pre>
 *
 * Controllers use {@code @RequestParam Fields fields} which Spring converts via a converter.
 * Response serialization happens through the configured Jackson {@link
 * org.hisp.dhis.fieldfiltering.better.FieldsConfig#jsonFilterMapper(org.hisp.dhis.fieldfiltering.better.FieldsPropertyFilter)}
 * bean.
 */
package org.hisp.dhis.fieldfiltering.better;
