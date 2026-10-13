/*
 * Copyright (c) 2004-2024, University of Oslo
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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exercises the metadata import and merge path of the JPA-mapped SMS special characters, which are
 * embedded in {@code smsCommands}.
 */
@Transactional
class SmsSpecialCharacterControllerTest extends PostgresControllerIntegrationTestBase {

  private static final String COMMAND_UID = "SmsCmdUid01";

  @Test
  void testImportPersistsSpecialCharacters() {
    importMetadata(command("{'name':'plus','value':'+'}", "{'name':'hash','value':'#'}"));

    assertEquals(Map.of("plus", "+", "hash", "#"), specialCharacters());
  }

  @Test
  void testReimportReplacesSpecialCharacters() {
    importMetadata(command("{'name':'plus','value':'+'}", "{'name':'hash','value':'#'}"));
    importMetadata(command("{'name':'star','value':'*'}"));

    assertEquals(Map.of("star", "*"), specialCharacters());
  }

  @Test
  void testPutOfExportedObjectRoundTrips() {
    importMetadata(command("{'name':'plus','value':'+'}", "{'name':'hash','value':'#'}"));
    String exported = GET("/smsCommands/" + COMMAND_UID).content(HttpStatus.OK).toJson();

    assertStatus(HttpStatus.OK, PUT("/smsCommands/" + COMMAND_UID, exported));

    assertEquals(Map.of("plus", "+", "hash", "#"), specialCharacters());
  }

  /** Same smsCommand property set and specialCharacters property as before the migration. */
  @Test
  void testSchemaProperties() {
    JsonObject schema = GET("/schemas/smsCommand").content(HttpStatus.OK);
    JsonArray properties = schema.getArray("properties");
    Set<String> names = new TreeSet<>();
    JsonObject specialCharacters = null;
    for (int i = 0; i < properties.size(); i++) {
      JsonObject property = properties.getObject(i);
      String name = property.getString("name").string();
      names.add(name);
      if ("specialCharacter".equals(name)) {
        specialCharacters = property;
      }
    }

    Set<String> expected =
        new TreeSet<>(
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
                "smsCode",
                "specialCharacter",
                "successMessage",
                "translation",
                "user",
                "userGroup",
                "wrongFormatMessage"));
    assertEquals(expected, names);
    assertNotNull(specialCharacters);
    assertEquals("specialCharacters", specialCharacters.getString("fieldName").string());
    assertEquals(
        "org.hisp.dhis.sms.command.SMSSpecialCharacter",
        specialCharacters.getString("itemKlass").string());
    assertEquals("COMPLEX", specialCharacters.getString("itemPropertyType").string());
    assertEquals("all", specialCharacters.getString("cascade").string());
    assertEquals(
        "org.hisp.dhis.sms.command.SMSCommand.specialCharacters",
        specialCharacters.getString("owningRole").string());
    assertEquals(true, specialCharacters.getBoolean("persisted").booleanValue());
    assertEquals(true, specialCharacters.getBoolean("owner").booleanValue());
    assertEquals(true, specialCharacters.getBoolean("manyToMany").booleanValue());
  }

  /** The special character is embedded in smsCommand only; it has no schema of its own. */
  @Test
  void testNoOwnSchema() {
    assertStatus(HttpStatus.NOT_FOUND, GET("/schemas/smsSpecialCharacter"));
  }

  private static String command(String... specialCharacters) {
    return ("{'smsCommands':[{'id':'%s','name':'CMD1','parserType':'KEY_VALUE_PARSER',"
            + "'specialCharacters':[%s]}]}")
        .formatted(COMMAND_UID, String.join(",", specialCharacters));
  }

  private Map<String, String> specialCharacters() {
    JsonArray characters =
        GET("/smsCommands/" + COMMAND_UID + "?fields=specialCharacters[name,value]")
            .content(HttpStatus.OK)
            .getArray("specialCharacters");
    Map<String, String> result = new HashMap<>();
    for (int i = 0; i < characters.size(); i++) {
      JsonObject character = characters.getObject(i);
      result.put(character.getString("name").string(), character.getString("value").string());
    }
    return result;
  }

  private void importMetadata(String metadata) {
    HttpResponse response = POST("/metadata", metadata);
    assertEquals(HttpStatus.OK, response.status(), () -> response.contentUnchecked().toJson());
  }
}
