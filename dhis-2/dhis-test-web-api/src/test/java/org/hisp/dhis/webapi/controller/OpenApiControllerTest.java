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

import static java.util.stream.Collectors.toSet;
import static org.hisp.dhis.http.HttpClientAdapter.Accept;
import static org.hisp.dhis.test.utils.Assertions.assertContains;
import static org.hisp.dhis.test.utils.Assertions.assertGreaterOrEqual;
import static org.hisp.dhis.test.utils.Assertions.assertLessOrEqual;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.TEXT_HTML_VALUE;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonMap;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonNodeType;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonString;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.webapi.openapi.OpenApiObject;
import org.hisp.dhis.webapi.openapi.OpenApiObject.ParameterObject;
import org.hisp.dhis.webapi.openapi.OpenApiObject.ResponseObject;
import org.hisp.dhis.webapi.openapi.OpenApiObject.SchemaObject;
import org.junit.jupiter.api.Test;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the {@link org.hisp.dhis.webapi.openapi.OpenApiController} with Mock MVC tests.
 *
 * <p>The documents returned by the controller are generated "on-the-fly" and are not dependent on
 * any database input.
 *
 * @author Jan Bernitt
 */
@Transactional
class OpenApiControllerTest extends H2ControllerIntegrationTestBase {

  @Test
  void testGetOpenApiDocumentJson() {
    JsonObject doc =
        GET("/openapi/openapi.json?failOnNameClash=true&failOnInconsistency=true").content();
    assertTrue(doc.isObject());
    assertTrue(doc.getObject("components.schemas.OrganisationUnitPropertyNames").isObject());
    assertGreaterOrEqual(150, doc.getObject("paths").size());
    assertGreaterOrEqual(0, doc.getObject("security[0].basicAuth").size());
    assertGreaterOrEqual(1, doc.getObject("components.securitySchemes").size());
    assertGreaterOrEqual(200, doc.getObject("components.schemas").size());
    assertGreaterOrEqual(200, doc.getObject("components.schemas").size());
    assertEquals(
        "#/components/schemas/TrackerTrackedEntity",
        doc.getObject("components.schemas.Body.properties.trackedEntities.items")
            .getString("$ref")
            .string());
  }

  @Test
  void testGetOpenApiDocumentJson_NoValidationErrors() {
    JsonObject doc =
        GET("/openapi/openapi.json?failOnNameClash=true&failOnInconsistency=true").content();
    SwaggerParseResult result =
        new OpenAPIParser().readContents(doc.node().getDeclaration(), null, null);
    assertEquals(List.of(), result.getMessages(), "There should not be any errors");
  }

  @Test
  void testGetOpenApiDocument_PathFilter() {
    JsonObject doc = GET("/openapi/openapi.json?scope=path:/api/users").content();
    assertTrue(doc.isObject());
    assertTrue(
        doc.getObject("paths")
            .has(
                "/api/users/gist",
                "/api/users/invite",
                "/api/users/invites",
                "/api/users/sharing"));
    assertLessOrEqual(26, doc.getObject("paths").size());
    assertLessOrEqual(100, doc.getObject("components.schemas").size());
  }

  @Test
  void testGetOpenApiDocument_ScopeFilter() {
    JsonObject doc = GET("/openapi/openapi.json?scope=entity:User").content();
    assertTrue(doc.isObject());
    assertTrue(
        doc.getObject("paths")
            .has(
                "/api/users/gist",
                "/api/users/invite",
                "/api/users/invites",
                "/api/users/sharing"));
    assertLessOrEqual(151, doc.getObject("paths").size());
    assertLessOrEqual(120, doc.getObject("components.schemas").size());
  }

  @Test
  void testGetOpenApiDocumentHtml_DomainFilter() {
    String html =
        GET("/openapi/openapi.html?scope=entity:DataElement", Accept(TEXT_HTML_VALUE))
            .content(TEXT_HTML_VALUE);
    assertContains("#DataElement", html);
  }

  @Test
  void testGetOpenApiDocument_DefaultValue() {
    // defaults in parameter objects (from Property analysis)
    JsonObject users = GET("/openapi/openapi.json?scope=path:/api/users").content();
    JsonObject sharedParams = users.getObject("components.parameters");
    assertEquals(50, sharedParams.getNumber("{GistParams.pageSize}.schema.default").integer());
    assertEquals(
        "AND", sharedParams.getString("{GistParams.rootJunction}.schema.default").string());
    assertTrue(sharedParams.getBoolean("{GistParams.translate}.schema.default").booleanValue());

    // defaults in individual parameters (from endpoint method parameter analysis)
    JsonObject fileResources = GET("/openapi/openapi.json?scope=path:/api/fileResources").content();
    JsonObject domain =
        fileResources
            .get("paths./api/fileResources/.post.parameters")
            .asList(JsonObject.class)
            .stream()
            .filter(p -> "domain".equals(p.getString("name").string()))
            .findFirst()
            .orElse(JsonMixed.of("{}"));
    assertEquals("DATA_VALUE", domain.getString("schema.default").string());

    JsonObject audits = GET("/openapi/openapi.json?scope=path:/api/audits").content();
    JsonObject pageSize =
        audits
            .getArray("paths./api/audits/trackedEntity.get.parameters")
            .asList(JsonObject.class)
            .stream()
            .filter(p -> "pageSize".equals(p.getString("name").string()))
            .findFirst()
            .orElse(JsonMixed.of("{}"));
    assertEquals(50, pageSize.getNumber("schema.default").integer());
  }

  /** Check shared parameter objects handling works */
  @Test
  void testGetOpenApiDocument_ParameterObjects() {
    OpenApiObject doc =
        GET("/openapi/openapi.json?scope=entity:OrganisationUnit")
            .content()
            .as(OpenApiObject.class);
    JsonList<ParameterObject> parameters =
        doc.$paths().get("/api/organisationUnits/").get().parameters();
    Set<String> allRefs =
        parameters.stream()
            .map(p -> p.getString("$ref"))
            .filter(JsonValue::exists)
            .map(JsonString::string)
            .collect(toSet());
    // check one of each group to make sure the inheritance handling works as expected
    assertTrue(
        allRefs.containsAll(
            Set.of(
                "#/components/parameters/GetObjectListParams.filter",
                "#/components/parameters/GetOrganisationUnitObjectListParams.level",
                "#/components/parameters/GetObjectParams.defaults")));
    // check "fields" is inlined (no reference) as it depend on the entity type
    assertTrue(parameters.stream().anyMatch(p -> "fields".equals(p.getString("name").string())));
  }

  /** Tests the "generics" handling of object list response */
  @Test
  void testGetOpenApiDocument_GetObjectListResponse() {
    OpenApiObject doc =
        GET("/openapi/openapi.json?scope=entity:OrganisationUnit")
            .content()
            .as(OpenApiObject.class);
    ResponseObject response =
        doc.$paths().get("/api/organisationUnits/").get().responses().get("200");
    JsonMap<SchemaObject> properties =
        response.content().get("application/json").schema().properties();

    assertEquals(
        Set.of("pager", "organisationUnits"),
        properties.keys().collect(toSet()),
        "there should only be a pager and an entity list property");

    SchemaObject listSchema = properties.get("organisationUnits");
    assertEquals("array", listSchema.$type());
    assertEquals(
        "#/components/schemas/OrganisationUnit", listSchema.items().getString("$ref").string());
  }

  @Test
  void testGetOpenApiDocument_ReadOnly() {
    JsonObject doc = GET("/openapi/openapi.json?scope=entity:JobConfiguration").content();
    JsonObject jobConfiguration = doc.getObject("components.schemas.JobConfiguration");
    JsonObject jobConfigurationParams = doc.getObject("components.schemas.JobConfigurationParams");
    assertTrue(jobConfiguration.isObject());
    assertTrue(jobConfigurationParams.isObject());
    assertTrue(
        jobConfiguration.getObject("properties").size()
            > jobConfigurationParams.getObject("properties").size());
    assertTrue(
        jobConfiguration
            .node()
            .find(JsonNodeType.BOOLEAN, n -> n.getPath().toString().endsWith("readOnly"))
            .isPresent());
    assertFalse(
        jobConfigurationParams
            .node()
            .find(JsonNodeType.BOOLEAN, n -> n.getPath().toString().endsWith("readOnly"))
            .isPresent());
  }

  @Test
  void testGetOpenApiDocument_CodeGeneration() throws IOException {
    JsonObject doc = GET("/openapi/openapi.json?failOnNameClash=true").content();

    Path tmpFile = Files.createTempFile("openapi", ".json");
    Files.writeString(tmpFile, doc.node().getDeclaration());

    CodegenConfigurator configurator =
        new CodegenConfigurator()
            .setInputSpec(tmpFile.toAbsolutePath().toString())
            .setGeneratorName("r");

    assertNotNull(
        new DefaultGenerator(true).opts(configurator.toClientOptInput()).generate(),
        "Like due to a query parameter which is complex, needs debugging to find out, insert breakpoint at RClientCodegen.constructExampleCode(RClientCodegen.java:950)");
  }
}
