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
package org.hisp.dhis.webapi.controller.userdatastore;

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.hisp.dhis.test.webapi.Assertions.assertWebMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import jakarta.persistence.EntityManagerFactory;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;
import org.hisp.dhis.userdatastore.UserDatastoreEntry;
import org.hisp.dhis.webapi.controller.UserDatastoreController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the {@link UserDatastoreController} using (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
@Transactional
class UserDatastoreControllerTest extends H2ControllerIntegrationTestBase {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private EntityManagerFactory entityManagerFactory;

  @Test
  void testDeleteKeys() {
    assertStatus(HttpStatus.CREATED, POST("/userDataStore/test/key1", "true"));
    assertWebMessage(
        "OK",
        200,
        "OK",
        "Namespace deleted: 'test'",
        DELETE("/userDataStore/test").content(HttpStatus.OK));
  }

  @Test
  void testDeleteKeys_NamespaceNotFound() {
    assertWebMessage(HttpStatus.NOT_FOUND, DELETE("/userDataStore/test"));
  }

  @Test
  void testAddUserKeyJsonValue() {
    assertWebMessage(
        "Created",
        201,
        "OK",
        "Key 'key1' in namespace 'test' created.",
        POST("/userDataStore/test/key1", "true").content(HttpStatus.CREATED));
  }

  @Test
  void testAddUserKeyJsonValue_MalformedValue() {
    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "Invalid JSON value for key 'key1'",
        POST("/userDataStore/test/key1", "invalidJson")
            .content(HttpStatus.BAD_REQUEST)
            .as(JsonWebMessage.class));
  }

  @Test
  void testAddUserKeyJsonValue_AlreadyExists() {
    assertStatus(HttpStatus.CREATED, POST("/userDataStore/test/key1", "true"));
    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        "Key 'key1' already exists in namespace 'test'",
        POST("/userDataStore/test/key1", "true")
            .content(HttpStatus.CONFLICT)
            .as(JsonWebMessage.class));
  }

  @Test
  void testPutUserKeyJsonValue() {
    assertWebMessage(
        "Created",
        201,
        "OK",
        "Key 'unknown' in namespace 'test' created.",
        PUT("/userDataStore/test/unknown", "false").content(HttpStatus.CREATED));
  }

  @Test
  void testUpdateUserKeyJsonValue_MalformedValue() {
    assertStatus(HttpStatus.CREATED, POST("/userDataStore/test/key1", "true"));
    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "Invalid JSON value for key 'key1'",
        PUT("/userDataStore/test/key1", "invalidJson")
            .error(HttpStatus.BAD_REQUEST)
            .as(JsonWebMessage.class));
  }

  @Test
  void testDeleteUserKeyJsonValue() {
    assertStatus(HttpStatus.CREATED, POST("/userDataStore/test/key1", "true"));
    assertWebMessage(
        "OK",
        200,
        "OK",
        "Key 'key1' deleted from the namespace 'test'.",
        DELETE("/userDataStore/test/key1").content(HttpStatus.OK));
  }

  @Test
  void testDeleteUserKeyJsonValue_UnknownKey() {
    assertWebMessage(
        "Not Found",
        404,
        "ERROR",
        "Key 'key1' not found in namespace 'test'",
        DELETE("/userDataStore/test/key1").content(HttpStatus.NOT_FOUND));
  }

  @Test
  void testPutEntry_EntryDoesNotExistAndIsCreated() {
    assertStatus(HttpStatus.CREATED, PUT("/userDataStore/test/mykey", "{\"name\":\"harry\"}"));
    JsonWebMessage myKey = GET("/userDataStore/test/mykey").content().as(JsonWebMessage.class);
    assertEquals("harry", myKey.getString("name").string());
  }

  @Test
  void testEncryptedEntryIsStoredEncryptedAndReturnedDecrypted() {
    assertStatus(
        HttpStatus.CREATED,
        POST("/userDataStore/secrets/pin?encrypt=true", "{\"pin\":\"s3cr3t-1234\"}"));
    dbmsManager.flushSession();
    dbmsManager.clearSession();
    entityManagerFactory.getCache().evict(UserDatastoreEntry.class);

    String stored =
        jdbcTemplate.queryForObject(
            "select encrypted_value from userkeyjsonvalue where namespace = 'secrets'",
            String.class);
    assertFalse(stored.contains("s3cr3t-1234"), "the value must not be stored in plain text");
    JsonWebMessage pin = GET("/userDataStore/secrets/pin").content().as(JsonWebMessage.class);
    assertEquals("s3cr3t-1234", pin.getString("pin").string());
  }
}
