/*
 * Copyright (c) 2004-2026, University of Oslo
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/** Tests the metadata import, export and schema of SMS commands. */
@Transactional
class SmsCommandMetadataControllerTest extends PostgresControllerIntegrationTestBase {
  private static final String COMMAND_UID = "SmsCommA001";

  /**
   * The schema properties of an SMS command except translations, which the schema names
   * "translation" and the JSON names "translations", and the collections, which the schema names by
   * their XML element.
   */
  private static final Set<String> PROPERTIES =
      Set.of(
          "access",
          "attributeValues",
          "code",
          "codeValueSeparator",
          "completenessMethod",
          "created",
          "createdBy",
          "currentPeriodUsedForReporting",
          "dataset",
          "defaultMessage",
          "displayName",
          "href",
          "id",
          "lastUpdated",
          "lastUpdatedBy",
          "moreThanOneOrgUnitMessage",
          "name",
          "noUserMessage",
          "parserType",
          "program",
          "programStage",
          "receivedMessage",
          "separator",
          "sharing",
          "successMessage",
          "user",
          "userGroup",
          "wrongFormatMessage");

  @Test
  void shouldCreateAndUpdateCommandViaMetadataImport() {
    importMetadata(command("Command A", "received A", "a"));

    JsonObject created = getCommand();
    assertEquals("Command A", created.getString("name").string());
    assertEquals("KEY_VALUE_PARSER", created.getString("parserType").string());
    assertEquals("DataSetA001", created.getObject("dataset").getString("id").string());
    assertEquals(".", created.getString("separator").string());
    assertEquals("=", created.getString("codeValueSeparator").string());
    assertEquals("default A", created.getString("defaultMessage").string());
    assertEquals("received A", created.getString("receivedMessage").string());
    assertEquals("success A", created.getString("successMessage").string());
    assertEquals("Wrong command format", created.getString("wrongFormatMessage").string());
    assertTrue(created.getBoolean("currentPeriodUsedForReporting").booleanValue());
    assertEquals("ALL_DATAVALUE", created.getString("completenessMethod").string());
    JsonObject code = created.getArray("smsCodes").getObject(0);
    assertEquals(1, created.getArray("smsCodes").size());
    assertEquals("a", code.getString("code").string());
    assertEquals("DataElmA001", code.getObject("dataElement").getString("id").string());
    assertTrue(code.getBoolean("compulsory").booleanValue());
    JsonObject specialCharacter = created.getArray("specialCharacters").getObject(0);
    assertEquals(1, created.getArray("specialCharacters").size());
    assertEquals("plus", specialCharacter.getString("name").string());
    assertEquals("+", specialCharacter.getString("value").string());

    importMetadata(command("Command B", "received B", "b"));

    JsonObject updated = getCommand();
    assertEquals("Command B", updated.getString("name").string());
    assertEquals("received B", updated.getString("receivedMessage").string());
    assertEquals(
        List.of("b"),
        updated.getArray("smsCodes").asList(JsonObject.class).stream()
            .map(c -> c.getString("code").string())
            .toList());
  }

  @Test
  void shouldUpdateCommandWhenExportedCommandWithChangesIsPut() {
    importMetadata(command("Command A", "received A", "a"));
    JsonObject exported = getCommand();
    JsonObject changed = withMember(exported, "name", "\"Command changed\"");
    changed = withMember(changed, "noUserMessage", "\"no user changed\"");

    assertStatus(HttpStatus.OK, PUT("/smsCommands/" + COMMAND_UID, changed.toJson()));

    JsonObject updated = getCommand();
    assertEquals("Command changed", updated.getString("name").string());
    assertEquals("no user changed", updated.getString("noUserMessage").string());
    assertEquals("received A", updated.getString("receivedMessage").string());
    assertEquals("DataSetA001", updated.getObject("dataset").getString("id").string());
    assertEquals("a", updated.getArray("smsCodes").getObject(0).getString("code").string());
    assertEquals(
        "+", updated.getArray("specialCharacters").getObject(0).getString("value").string());
  }

  @Test
  void shouldPatchCommandName() {
    importMetadata(command("Command A", "received A", "a"));

    assertStatus(
        HttpStatus.OK,
        PATCH(
            "/smsCommands/" + COMMAND_UID,
            "[{'op':'replace','path':'/name','value':'Command patched'}]"));

    JsonObject patched = getCommand();
    assertEquals("Command patched", patched.getString("name").string());
    assertEquals("received A", patched.getString("receivedMessage").string());
    assertEquals("a", patched.getArray("smsCodes").getObject(0).getString("code").string());
    assertEquals(
        "+", patched.getArray("specialCharacters").getObject(0).getString("value").string());
  }

  @Test
  void shouldDeleteCommand() {
    importMetadata(command("Command A", "received A", "a"));

    assertStatus(HttpStatus.OK, DELETE("/smsCommands/" + COMMAND_UID));

    assertEquals(HttpStatus.NOT_FOUND, getCommandResponse().status());
  }

  @Test
  void shouldKeepProgramAndStageOfCommandImportedWithThem() {
    importMetadata(
        """
        {'programs':[{
          'id':'ProgramA001','name':'Program A','shortName':'Program A',
          'programType':'WITHOUT_REGISTRATION'
        }],
        'programStages':[{'id':'PrStageA001','name':'Stage A','program':{'id':'ProgramA001'}}],
        'smsCommands':[{
          'id':'%s','name':'Command A','parserType':'PROGRAM_STAGE_DATAENTRY_PARSER',
          'program':{'id':'ProgramA001'},'programStage':{'id':'PrStageA001'}
        }]}
        """
            .formatted(COMMAND_UID));

    JsonObject command = getCommand();
    assertEquals("ProgramA001", command.getObject("program").getString("id").string());
    assertEquals("PrStageA001", command.getObject("programStage").getString("id").string());
  }

  @Test
  void shouldRejectTranslationsOfCommandOnMetadataImport() {
    HttpResponse response =
        POST(
            "/metadata",
            """
            {'smsCommands':[{
              'id':'%s','name':'Command A','parserType':'ALERT_PARSER',
              'translations':[{'locale':'fr','property':'NAME','value':'Commande A'}]
            }]}
            """
                .formatted(COMMAND_UID));

    String report = response.contentUnchecked().toJson();
    assertEquals(HttpStatus.CONFLICT, response.status(), report);
    assertTrue(report.contains("E1107"), report);
    assertEquals(HttpStatus.NOT_FOUND, getCommandResponse().status());
  }

  @Test
  void shouldExportCommandProperties() {
    importMetadata(command("Command A", "received A", "a"));

    JsonObject command = getCommand();

    Set<String> names = new TreeSet<>(PROPERTIES);
    names.removeAll(
        Set.of(
            "code", "createdBy", "lastUpdatedBy", "user", "userGroup", "program", "programStage"));
    names.addAll(Set.of("translations", "smsCodes", "specialCharacters"));
    assertEquals(names, new TreeSet<>(command.names()));
    assertTrue(command.getString("href").string().endsWith("/smsCommands/" + COMMAND_UID));
    assertEquals("Command A", command.getString("displayName").string());
    assertEquals(Set.of("users", "userGroups"), Set.copyOf(command.getObject("sharing").names()));
    assertTrue(command.getArray("attributeValues").isEmpty());
  }

  @Test
  void shouldDescribeCommandSchemaPropertyNames() {
    Set<String> names = new TreeSet<>(PROPERTIES);
    names.addAll(Set.of("translation", "smsCode", "specialCharacter"));
    assertEquals(names, schemaPropertyNames(property -> true));
  }

  @Test
  void shouldDescribeCommandSchemaPersistedProperties() {
    assertEquals(
        Set.of(
            "codeValueSeparator",
            "completenessMethod",
            "created",
            "currentPeriodUsedForReporting",
            "dataset",
            "defaultMessage",
            "id",
            "lastUpdated",
            "moreThanOneOrgUnitMessage",
            "name",
            "noUserMessage",
            "parserType",
            "program",
            "programStage",
            "receivedMessage",
            "separator",
            "smsCode",
            "specialCharacter",
            "successMessage",
            "userGroup",
            "wrongFormatMessage"),
        schemaPropertyNames(property -> property.getBoolean("persisted").booleanValue()));
  }

  @Test
  void shouldDescribeCommandSchemaTypesLengthsAndCascades() {
    Map<String, JsonObject> properties =
        schemaProperties().collect(Collectors.toMap(p -> p.getString("name").string(), p -> p));

    assertEquals(Integer.MAX_VALUE, properties.get("name").getNumber("length").intValue());
    assertEquals(
        Integer.MAX_VALUE, properties.get("receivedMessage").getNumber("length").intValue());
    assertEquals(50, properties.get("parserType").getNumber("length").intValue());
    assertFalse(properties.get("id").getBoolean("unique").booleanValue());
    assertEquals(
        "CONSTANT", properties.get("completenessMethod").getString("propertyType").string());
    assertEquals("all", properties.get("smsCode").getString("cascade").string());
    assertEquals("all", properties.get("specialCharacter").getString("cascade").string());
    assertEquals("URL", properties.get("href").getString("propertyType").string());
  }

  @Test
  void shouldDescribeCommandSchemaNameAndFlags() {
    JsonObject schema = GET("/schemas/smsCommand").content(HttpStatus.OK);

    assertEquals("smscommand", schema.getString("name").string());
    assertTrue(schema.getBoolean("metadata").booleanValue());
    assertFalse(schema.getBoolean("shareable").booleanValue());
    assertFalse(schema.getBoolean("translatable").booleanValue());
  }

  private Set<String> schemaPropertyNames(Predicate<JsonObject> filter) {
    return schemaProperties()
        .filter(filter)
        .map(property -> property.getString("name").string())
        .collect(Collectors.toCollection(TreeSet::new));
  }

  private Stream<JsonObject> schemaProperties() {
    return GET("/schemas/smsCommand")
        .content(HttpStatus.OK)
        .getArray("properties")
        .asList(JsonObject.class)
        .stream();
  }

  private static String command(String name, String receivedMessage, String code) {
    return """
        {'dataElements':[{
          'id':'DataElmA001','name':'Data element A','shortName':'Data element A',
          'valueType':'INTEGER','domainType':'AGGREGATE','aggregationType':'SUM'
        }],
        'dataSets':[{
          'id':'DataSetA001','name':'Data set A','shortName':'Data set A','periodType':'Monthly'
        }],
        'smsCommands':[{
          'id':'%s','name':'%s','parserType':'KEY_VALUE_PARSER','dataset':{'id':'DataSetA001'},
          'separator':'.','codeValueSeparator':'=','defaultMessage':'default A',
          'receivedMessage':'%s','successMessage':'success A',
          'currentPeriodUsedForReporting':true,'completenessMethod':'ALL_DATAVALUE',
          'smsCodes':[{'code':'%s','dataElement':{'id':'DataElmA001'},'compulsory':true}],
          'specialCharacters':[{'name':'plus','value':'+'}]
        }]}
        """
        .formatted(COMMAND_UID, name, receivedMessage, code);
  }

  /** Returns a copy of the object with the value of the given member replaced. */
  private static JsonObject withMember(JsonObject object, String member, String json) {
    return JsonMixed.of(object.get(member).node().replaceWith(json)).asObject();
  }

  private void importMetadata(String metadata) {
    HttpResponse response = POST("/metadata", metadata);
    assertEquals(HttpStatus.OK, response.status(), () -> response.contentUnchecked().toJson());
    assertEquals("OK", response.content().getString("status").string());
  }

  private JsonObject getCommand() {
    return getCommandResponse().content(HttpStatus.OK);
  }

  /** Requests the command in a new session. */
  private HttpResponse getCommandResponse() {
    startNewRequestSession();
    return GET("/smsCommands/" + COMMAND_UID);
  }
}
