/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.hisp.dhis.http.HttpClientAdapter.Body;
import static org.hisp.dhis.http.HttpClientAdapter.ContentType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.hisp.dhis.attribute.Attribute.ObjectType;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonGenerator;
import org.hisp.dhis.test.webapi.json.domain.JsonIdentifiableObject;
import org.hisp.dhis.test.webapi.json.domain.JsonSchema;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

/**
 * This tests uses the {@link JsonSchema} information the server provides to create an object for
 * each {@link org.hisp.dhis.schema.Schema} (some won't work) and then delete it again.
 *
 * <p>When objects depend upon other objects these are created first.
 *
 * @author Jan Bernitt
 */
@Transactional
class SchemaBasedControllerTest extends PostgresControllerIntegrationTestBase {

  private static final Set<String> IGNORED_SCHEMAS =
      Set.of(
          "externalFileResource", // can't POST files
          "categoryOptionCombo", // can't POST/create a new COC, should use /metadata
          "identifiableObject", // depends on files
          "dashboard", // uses JSONB functions (improve test setup)
          "pushanalysis", // uses dashboards (see above)
          "metadataVersion", // no POST endpoint
          "softDeletableObject", // depends on programInstance (see above)
          "relationship", // generator insufficient for embedded fields
          "relationshipType", // generator insufficient for embedded fields
          "eventFilter", // generator insufficient
          "interpretation", // required ObjectReport not required in schema
          "user", // generator insufficient to understand user
          "jobConfiguration", // API requires configurable=true
          "messageConversation", // needs recipients (not a required field)
          "programRuleAction", // needs DataElement and TrackedEntityAttribute
          "validationRule", // generator insufficient (embedded fields)
          "programStage", // body request does not include mandatory field programId
          "programStageWorkingList", // same reason as programStage
          "dataElement", // non-postgres SQL in deletion handler
          "predictor", // NPE in preheat when creating objects
          "aggregateDataExchange", // required JSONB objects not working,
          "oauth2Authorization", // Read-only
          "oauth2AuthorizationConsent" // Read-only
          );

  /**
   * A list of endpoints that do not support the {@code /gist} API because their controller does not
   * extend the base class that implements it.
   */
  private static final Set<String> IGNORED_GIST_ENDPOINTS = Set.of("reportTable", "chart");

  private final Map<String, String> createdObjectIds = new ConcurrentHashMap<>();

  @TestFactory
  Stream<DynamicNode> generateSchemaBasedTests() {
    JsonList<JsonSchema> schemas = GET("/schemas").content().getList("schemas", JsonSchema.class);
    JsonGenerator generator = new JsonGenerator(schemas);
    List<DynamicNode> tests = new ArrayList<>();
    for (JsonSchema schema : schemas) {
      if (!isExcludedFromTest(schema)) {
        tests.add(
            dynamicContainer(
                schema.getRelativeApiEndpoint(), generatorTestsForSchema(schema, generator)));
      }
    }
    assertTrue(tests.size() > 50, "There should be around 50 schemas");
    return tests.stream();
  }

  private List<DynamicNode> generatorTestsForSchema(JsonSchema schema, JsonGenerator generator) {
    return List.of(
        dynamicTest("create object", () -> runCreateObject(schema, generator)),
        dynamicTest("patch object", () -> runPatchObject(schema)),
        dynamicTest("query object", () -> runQueryObject(schema)),
        dynamicTest("query object list", () -> runQueryObjectList(schema)),
        dynamicTest("query object Gist", () -> runQueryObjectGist(schema)),
        dynamicTest("query object list Gist", () -> runQueryObjectListGist(schema)),
        dynamicTest("update object attribute value", () -> runUpdateAttributeValue(schema)),
        dynamicTest("delete object", () -> runDeleteObject(schema)));
  }

  private void runCreateObject(JsonSchema schema, JsonGenerator generator) {
    Map<String, String> objects = generator.generateObjects(schema);
    String uid = "";
    // create needed object(s)
    // last created is the one we want to test for schema
    // those before might be objects it depends upon that
    // need to be created first
    for (Entry<String, String> entry : objects.entrySet()) {
      uid = assertStatus(HttpStatus.CREATED, POST(entry.getKey(), entry.getValue()));
    }
    String endpoint = schema.getRelativeApiEndpoint();
    createdObjectIds.put(endpoint, uid);
  }

  private void runPatchObject(JsonSchema schema) {
    String endpoint = schema.getRelativeApiEndpoint();
    String uid = createdObjectIds.get(endpoint);
    @Language("json")
    String json =
        """
    [{ "op": "add", "path": "/name", "value": "new_name_patch" }]""";
    assertStatus(HttpStatus.OK, PATCH(endpoint + "/" + uid, json));
  }

  private void runDeleteObject(JsonSchema schema) {
    String endpoint = schema.getRelativeApiEndpoint();
    String uid = createdObjectIds.get(endpoint);
    assertStatus(HttpStatus.OK, DELETE(endpoint + "/" + uid));
  }

  private void runQueryObjectListGist(JsonSchema schema) {
    String name = schema.getName();
    assumeFalse(
        IGNORED_GIST_ENDPOINTS.contains(schema.getName()), name + " ignores Gist API for reasons");

    String endpoint = schema.getRelativeApiEndpoint();
    String uid = createdObjectIds.get(endpoint);

    // test gist list of object for the schema
    JsonObject gist = GET(endpoint + "/gist").content();
    assertTrue(gist.getObject("pager").exists());
    JsonArray list = gist.getArray(schema.getPlural());
    assertFalse(list.isEmpty());
    // only if there is only one we are sure it is the one we created
    if (list.size() == 1) {
      assertEquals(uid, list.getObject(0).getString("id").string());
    }
  }

  private void runQueryObjectGist(JsonSchema schema) {
    String name = schema.getName();
    assumeFalse(
        IGNORED_GIST_ENDPOINTS.contains(schema.getName()), name + " ignores Gist API for reasons");

    String endpoint = schema.getRelativeApiEndpoint();
    String uid = createdObjectIds.get(endpoint);

    JsonObject object = GET(endpoint + "/" + uid + "/gist").content();
    assertTrue(object.exists());
    assertEquals(uid, object.getString("id").string());
  }

  private void runQueryObject(JsonSchema schema) {
    String name = schema.getName();
    assumeFalse(
        IGNORED_GIST_ENDPOINTS.contains(schema.getName()), name + " ignores Gist API for reasons");

    String endpoint = schema.getRelativeApiEndpoint();
    String uid = createdObjectIds.get(endpoint);

    JsonObject object = GET(endpoint + "/" + uid).content();
    assertTrue(object.exists());
    assertEquals(uid, object.getString("id").string());
  }

  private void runQueryObjectList(JsonSchema schema) {
    String endpoint = schema.getRelativeApiEndpoint();
    String uid = createdObjectIds.get(endpoint);

    // test list of object for the schema
    JsonObject gist = GET(endpoint).content();
    assertTrue(gist.getObject("pager").exists());
    JsonArray list = gist.getArray(schema.getPlural());
    assertFalse(list.isEmpty());
    // only if there is only one we are sure it is the one we created
    if (list.size() == 1) {
      assertEquals(uid, list.getObject(0).getString("id").string());
    }
  }

  private void runUpdateAttributeValue(JsonSchema schema) {
    ObjectType type = ObjectType.valueOf(schema.getKlass());
    if (type == null || type == ObjectType.MAP) return;

    String endpoint = schema.getRelativeApiEndpoint();
    String uid = createdObjectIds.get(endpoint);

    String attrId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/attributes",
                "{'name':'"
                    + type
                    + "', 'valueType':'INTEGER','"
                    + type.getPropertyName()
                    + "':true}"));

    JsonObject object = GET(endpoint + "/" + uid).content();
    assertStatus(
        HttpStatus.OK,
        PUT(
            endpoint + "/" + uid + "?mergeMode=REPLACE",
            Body(
                object
                    .getObject("attributeValues")
                    .node()
                    .replaceWith("[{\"value\":\"42\", \"attribute\":{\"id\":\"" + attrId + "\"}}]")
                    .getDeclaration()),
            ContentType(MediaType.APPLICATION_JSON)));
    assertEquals(
        "42",
        GET(endpoint + "/" + uid)
            .content()
            .as(JsonIdentifiableObject.class)
            .getAttributeValues()
            .get(0)
            .getValue());
  }

  private boolean isExcludedFromTest(JsonSchema schema) {
    return schema.isEmbeddedObject()
        || !schema.isIdentifiableObject()
        || !schema.getApiEndpoint().exists()
        || IGNORED_SCHEMAS.contains(schema.getName());
  }
}
