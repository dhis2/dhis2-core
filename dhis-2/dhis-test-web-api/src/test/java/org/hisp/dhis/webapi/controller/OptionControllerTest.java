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
import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonIdentifiableObject;
import org.hisp.dhis.test.webapi.json.domain.JsonOption;
import org.hisp.dhis.test.webapi.json.domain.JsonOptionSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class OptionControllerTest extends PostgresControllerIntegrationTestBase {

  private static final String OPTION_SET_UID = "OptSetUid01";

  private static final String OPTION_A_UID = "OptionUid01";

  private static final String OPTION_B_UID = "OptionUid02";

  private static final String ATTRIBUTE_UID = "AttrUid0001";

  @Autowired private EntityManagerFactory entityManagerFactory;

  @Test
  void testUpdateOptionWithSortOrderGap() {
    // Create OptionSet with two Options
    POST(
            "/metadata",
            "{\"optionSets\":\n"
                + "    [{\"name\": \"Device category\",\"id\": \"RHqFlB1Wm4d\",\"version\": 2,\"valueType\": \"TEXT\"}]}")
        .content(HttpStatus.OK);
    POST(
            "/metadata",
            "{ \"options\":"
                + "    [{\"code\": \"Vaccine freezer\",\"name\": \"Vaccine freezer\",\"id\": \"BQMei56UBl6\",\"sortOrder\": 1,\"optionSet\":{\"id\": \"RHqFlB1Wm4d\"}},\n"
                + "    {\"code\": \"Icelined refrigerator\",\"name\": \"Icelined refrigerator\",\"id\": \"Uh4HvjK6zg3\",\"sortOrder\": 2,\"optionSet\":{\"id\": \"RHqFlB1Wm4d\"}}]}")
        .content(HttpStatus.OK);

    JsonObject response =
        GET("/optionSets/{uid}?fields=options[id,sortOrder]", "RHqFlB1Wm4d").content();

    // sortOrder is 1 and 2
    assertEquals(2, response.getObject("options").size());
    assertEquals(1, response.getNumber("options[0].sortOrder").intValue());
    assertEquals(2, response.getNumber("options[1].sortOrder").intValue());

    // Update option sortOrder 2 to 20
    POST(
            "/metadata",
            "{\"options\":\n"
                + "[{\"code\": \"Icelined refrigerator\",\"name\": \"Icelined refrigerator\",\"id\": \"Uh4HvjK6zg3\",\"sortOrder\": 20,\"optionSet\":{\"id\": \"RHqFlB1Wm4d\"}}]}")
        .content(HttpStatus.OK);

    response = GET("/optionSets/{uid}?fields=options[id,sortOrder]", "RHqFlB1Wm4d").content();
    assertEquals(2, response.getObject("options").size());
    assertEquals(1, response.getNumber("options[0].sortOrder").intValue());
    assertEquals("Uh4HvjK6zg3", response.getString("options[1].id").string());
    assertEquals(20, response.getNumber("options[1].sortOrder").intValue());
  }

  @Test
  void testImportOptionWithoutSortOrder() {
    String id =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/optionSets/",
                "{'name': 'test', 'version': 2, 'valueType': 'TEXT', 'description':'desc' }"));
    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/options/",
            "{'optionSet': { 'id':'"
                + id
                + "'}, 'id':'Uh4HvjK6zg3', 'code': 'A', 'name': 'Anna', 'description': 'this-is-a'}"));
    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/options/",
            "{'optionSet': { 'id':'"
                + id
                + "'},'id':'BQMei56UBl6','code': 'B', 'name': 'Betta', 'description': 'this-is-b'}"));
    JsonOptionSet set =
        GET("/optionSets/{id}?fields=options[id,sortOrder]", id).content().as(JsonOptionSet.class);
    assertEquals("Uh4HvjK6zg3", set.getOptions().get(0).getId());
    assertEquals(0, set.getOptions().get(0).getSortOrder());
    assertEquals("BQMei56UBl6", set.getOptions().get(1).getId());
    assertEquals(0, set.getOptions().get(1).getSortOrder());
  }

  @Test
  void testOptionSetsWithDescription() {
    String id =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/optionSets/",
                "{'name': 'test', 'version': 2, 'valueType': 'TEXT', 'description':'desc' }"));
    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/options/",
            "{'optionSet': { 'id':'"
                + id
                + "'}, 'code': 'A', 'name': 'Anna', 'description': 'this-is-a', 'sortOrder': 1}"));
    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/options/",
            "{'optionSet': { 'id':'"
                + id
                + "'},'code': 'B', 'name': 'Betta', 'description': 'this-is-b', 'sortOrder': 2}"));

    JsonOptionSet set =
        GET("/optionSets/{id}?fields=id,name,description,options[id,name,description]", id)
            .content()
            .as(JsonOptionSet.class);
    assertEquals("desc", set.getDescription());
    assertEquals(
        List.of("this-is-a", "this-is-b"),
        set.getOptions().toList(JsonIdentifiableObject::getDescription));

    JsonList<JsonOption> options =
        GET("/options/gist?headless=true&translate=false&filter=optionSet.id:eq:{id}", id)
            .content()
            .asList(JsonOption.class);
    assertEquals(2, options.size());
    assertEquals(List.of("Anna", "Betta"), options.toList(JsonIdentifiableObject::getName));
  }

  @Test
  void testImportedOptionExportsEveryMappedProperty() {
    importOptionSet("Anna", "desc a", "#ff0000");

    JsonObject option =
        GET(
                "/options/{uid}?fields=code,name,sortOrder,description,formName,style,"
                    + "translations,attributeValues[value,attribute[id]],optionSet[id]",
                OPTION_A_UID)
            .content();
    assertEquals("A", option.getString("code").string());
    assertEquals("Anna", option.getString("name").string());
    assertEquals(0, option.getNumber("sortOrder").intValue());
    assertEquals("desc a", option.getString("description").string());
    assertEquals("form a", option.getString("formName").string());
    assertEquals("#ff0000", option.getString("style.color").string());
    assertEquals("icon-a", option.getString("style.icon").string());
    assertEquals("Anne", option.getString("translations[0].value").string());
    assertEquals("attribute value", option.getString("attributeValues[0].value").string());
    assertEquals(ATTRIBUTE_UID, option.getString("attributeValues[0].attribute.id").string());
    assertEquals(OPTION_SET_UID, option.getString("optionSet.id").string());
  }

  @Test
  void testReimportUpdatesTheOptions() {
    importOptionSet("Anna", "desc a", "#ff0000");
    importOptionSet("Anna renamed", "desc changed", "#00ff00");

    JsonObject option = GET("/options/{uid}?fields=name,description,style", OPTION_A_UID).content();
    assertEquals("Anna renamed", option.getString("name").string());
    assertEquals("desc changed", option.getString("description").string());
    assertEquals("#00ff00", option.getString("style.color").string());
    JsonObject optionSet =
        GET("/optionSets/{uid}?fields=options[id,sortOrder]", OPTION_SET_UID).content();
    assertEquals(
        List.of(OPTION_A_UID, OPTION_B_UID),
        optionSet.getList("options", JsonObject.class).toList(o -> o.getString("id").string()));
  }

  @Test
  void testPutOfTheExportedOptionUpdatesFormNameAndKeepsTheRest() {
    importOptionSet("Anna", "desc a", "#ff0000");
    JsonObject exported = GET("/options/{uid}", OPTION_A_UID).content();

    String body =
        exported
            .getString("formName")
            .node()
            .replaceWith("\"form changed\"")
            .getDeclaration()
            .toString();
    assertStatus(HttpStatus.OK, PUT("/options/" + OPTION_A_UID, body));

    newRequest();
    JsonObject option =
        GET(
                "/options/{uid}?fields=formName,description,style,translations,"
                    + "attributeValues[value],optionSet[id]",
                OPTION_A_UID)
            .content();
    assertEquals("form changed", option.getString("formName").string());
    assertEquals("desc a", option.getString("description").string());
    assertEquals("#ff0000", option.getString("style.color").string());
    assertEquals("Anne", option.getString("translations[0].value").string());
    assertEquals("attribute value", option.getString("attributeValues[0].value").string());
    assertEquals(OPTION_SET_UID, option.getString("optionSet.id").string());
  }

  @Test
  void testOptionGetReturnsTheExpectedKeys() {
    importOptionSet("Anna", "desc a", "#ff0000");

    assertContainsOnly(
        Set.of(
            "access",
            "attributeValues",
            "code",
            "created",
            "description",
            "displayDescription",
            "displayFormName",
            "displayName",
            "formName",
            "href",
            "id",
            "lastUpdated",
            "name",
            "optionSet",
            "sharing",
            "sortOrder",
            "style",
            "translations"),
        GET("/options/{uid}", OPTION_A_UID).content().names());
  }

  @Test
  void testOptionsNestedInTheOptionSetKeepTheirDisplayName() {
    importOptionSet("Anna", "desc a", "#ff0000");

    JsonObject optionSet =
        GET("/optionSets/{uid}?fields=options[id,code,displayName]", OPTION_SET_UID).content();
    assertEquals("Anna", optionSet.getString("options[0].displayName").string());
    assertEquals("B", optionSet.getString("options[1].code").string());
    JsonArray defaultOptions =
        GET("/optionSets/{uid}", OPTION_SET_UID).content().getArray("options");
    assertContainsOnly(Set.of("id"), defaultOptions.getObject(0).names());
  }

  @Test
  void testPatchOfTheOptionSetKeepsItsOptions() {
    importOptionSet("Anna", "desc a", "#ff0000");

    assertStatus(
        HttpStatus.OK,
        PATCH(
            "/optionSets/" + OPTION_SET_UID,
            "[{'op':'replace','path':'/description','value':'patched'}]"));

    newRequest();
    JsonObject optionSet =
        GET("/optionSets/{uid}?fields=description,options[id,name]", OPTION_SET_UID).content();
    assertEquals("patched", optionSet.getString("description").string());
    assertEquals(
        List.of("Anna", "Betta"),
        optionSet.getList("options", JsonObject.class).toList(o -> o.getString("name").string()));
  }

  @Test
  void testSchemaExposesTheOptionProperties() {
    JsonObject schema = GET("/schemas/option").content();

    assertEquals("option", schema.getString("name").string());
    JsonList<JsonObject> properties = schema.getList("properties", JsonObject.class);
    assertContainsOnly(
        Set.of(
            "access",
            "attributeValues",
            "code",
            "created",
            "createdBy",
            "description",
            "displayDescription",
            "displayFormName",
            "displayName",
            "displayShortName",
            "formName",
            "href",
            "id",
            "lastUpdated",
            "lastUpdatedBy",
            "name",
            "optionSet",
            "sharing",
            "shortName",
            "sortOrder",
            "style",
            "translation",
            "user"),
        properties.toList(p -> p.getString("name").string()));
  }

  @Test
  void testSchemaReportsTheColumnLengths() {
    JsonObject schema = GET("/schemas/option").content();

    assertEquals(
        Integer.MAX_VALUE, schemaProperty(schema, "description").getNumber("length").intValue());
    assertEquals(
        Integer.MAX_VALUE, schemaProperty(schema, "formName").getNumber("length").intValue());
    assertEquals(230, schemaProperty(schema, "code").getNumber("length").intValue());
    assertEquals(230, schemaProperty(schema, "name").getNumber("length").intValue());
  }

  @Test
  void testSchemaReportsTheUnsupportedPropertiesAsNotPersisted() {
    JsonObject schema = GET("/schemas/option").content();

    for (String name : List.of("shortName", "createdBy", "lastUpdatedBy", "sharing")) {
      assertFalse(schemaProperty(schema, name).getBoolean("persisted").booleanValue(), name);
    }
  }

  @Test
  void testOptionsNestedInAnOptionGroupKeepTheirShape() {
    importOptionSet("Anna", "desc a", "#ff0000");
    POST(
            "/metadata",
            """
            {"optionGroups":[{"id":"OptGroup001","name":"Group","shortName":"Group",
              "optionSet":{"id":"%s"},"options":[{"id":"%s"}]}]}"""
                .formatted(OPTION_SET_UID, OPTION_A_UID))
        .content(HttpStatus.OK);
    newRequest();

    JsonArray defaultOptions = GET("/optionGroups/OptGroup001").content().getArray("options");
    assertContainsOnly(Set.of("id"), defaultOptions.getObject(0).names());
    JsonObject group =
        GET("/optionGroups/OptGroup001?fields=options[id,code,displayName]").content();
    assertEquals("Anna", group.getString("options[0].displayName").string());
    assertEquals("A", group.getString("options[0].code").string());
  }

  @Test
  void testOptionOfADataElementOptionDimensionItemKeepsItsShape() {
    importOptionSet("Anna", "desc a", "#ff0000");
    POST(
            "/metadata",
            """
            {"programs":[{"id":"ProgramUid1","name":"Program","shortName":"Program",
               "programType":"WITHOUT_REGISTRATION"}],
             "dataElements":[{"id":"DataElemUid","name":"Data element","shortName":"Data element",
               "valueType":"TEXT","domainType":"TRACKER","aggregationType":"NONE",
               "optionSet":{"id":"%s"}}]}"""
                .formatted(OPTION_SET_UID))
        .content(HttpStatus.OK);
    String visualization =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/visualizations/",
                """
                {"name":"Option item","type":"PIE","columns":[{"dimension":"dx","items":[
                  {"id":"ProgramUid1.DataElemUid.%s","dimensionItemType":"PROGRAM_DATA_ELEMENT_OPTION"}]}]}"""
                    .formatted(OPTION_A_UID)));
    newRequest();

    String itemPath = "dataDimensionItems[0].programDataElementOption.option";
    JsonObject defaultOption =
        GET(
                "/visualizations/{uid}?fields=dataDimensionItems[programDataElementOption[option]]",
                visualization)
            .content()
            .getObject(itemPath);
    assertContainsOnly(Set.of("id"), defaultOption.names());
    JsonObject option =
        GET(
                "/visualizations/{uid}?fields=dataDimensionItems[programDataElementOption["
                    + "option[id,code,displayName]]]",
                visualization)
            .content()
            .getObject(itemPath);
    assertEquals("Anna", option.getString("displayName").string());
    assertEquals("A", option.getString("code").string());
  }

  private void importOptionSet(String nameA, String descriptionA, String colorA) {
    JsonObject report =
        POST(
                "/metadata",
                """
                {"attributes":[{"id":"%s","name":"Option attribute","valueType":"TEXT",
                   "optionAttribute":true}],
                 "optionSets":[{"id":"%s","name":"Option set","valueType":"TEXT",
                   "options":[{"id":"%s"},{"id":"%s"}]}],
                 "options":[
                   {"id":"%s","code":"A","name":"%s","sortOrder":1,"description":"%s",
                    "formName":"form a","style":{"color":"%s","icon":"icon-a"},
                    "translations":[{"locale":"fr","property":"NAME","value":"Anne"}],
                    "attributeValues":[{"value":"attribute value","attribute":{"id":"%s"}}],
                    "optionSet":{"id":"%s"}},
                   {"id":"%s","code":"B","name":"Betta","sortOrder":2,
                    "optionSet":{"id":"%s"}}]}"""
                    .formatted(
                        ATTRIBUTE_UID,
                        OPTION_SET_UID,
                        OPTION_A_UID,
                        OPTION_B_UID,
                        OPTION_A_UID,
                        nameA,
                        descriptionA,
                        colorA,
                        ATTRIBUTE_UID,
                        OPTION_SET_UID,
                        OPTION_B_UID,
                        OPTION_SET_UID))
            .content(HttpStatus.OK);
    assertEquals("OK", report.getString("status").string());
    newRequest();
  }

  private static JsonObject schemaProperty(JsonObject schema, String name) {
    return schema.getList("properties", JsonObject.class).stream()
        .filter(p -> name.equals(p.getString("name").string()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("no schema property " + name));
  }

  /** Flushes and clears the session and the second-level cache, as between two requests. */
  private void newRequest() {
    startNewRequestSession();
    entityManagerFactory.getCache().evict(Option.class);
  }
}
