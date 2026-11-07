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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * Guards the metadata import, merge and Jackson path of the JPA-mapped SMS special characters,
 * which are embedded in {@code smsCommands}.
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

  /** The specialCharacters property of smsCommand is unchanged by the migration. */
  @Test
  void testSchemaProperty() {
    JsonArray properties = GET("/schemas/smsCommand").content(HttpStatus.OK).getArray("properties");
    JsonObject property = null;
    for (int i = 0; i < properties.size(); i++) {
      if ("specialCharacter".equals(text(properties.getObject(i), "name"))) {
        property = properties.getObject(i);
      }
    }

    assertNotNull(property);
    assertEquals("specialCharacters", text(property, "fieldName"));
    assertEquals("org.hisp.dhis.sms.command.SMSSpecialCharacter", text(property, "itemKlass"));
    assertEquals("COMPLEX", text(property, "itemPropertyType"));
    assertEquals("all", text(property, "cascade"));
    assertEquals(
        "org.hisp.dhis.sms.command.SMSCommand.specialCharacters", text(property, "owningRole"));
    assertTrue(property.getBoolean("persisted").booleanValue());
    assertTrue(property.getBoolean("owner").booleanValue());
    assertTrue(property.getBoolean("manyToMany").booleanValue());
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
      result.put(text(character, "name"), text(character, "value"));
    }
    return result;
  }

  private static String text(JsonObject object, String key) {
    return object.getString(key).string();
  }

  private void importMetadata(String metadata) {
    HttpResponse response = POST("/metadata", metadata);
    assertEquals(HttpStatus.OK, response.status(), () -> response.contentUnchecked().toJson());
  }
}
