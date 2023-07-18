/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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

import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.jsontree.JsonBoolean;
import org.hisp.dhis.user.User;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the special ability of a superuser to impersonate other users when using the {@link
 * UserDatastoreController} API.
 *
 * @author Jan Bernitt
 */
class UserDatastoreSuperuserControllerTest extends DhisControllerConvenienceTest {
  private User paul;

  @BeforeEach
  void setUp() {
    // All tests start as user Paul
    paul = switchToNewUser("Paul");
  }

  @Test
  void testDeleteKeys() {
    assertStatus(HttpStatus.CREATED, POST("/userDataStore/test/key1", "true"));

    switchToSuperuser();
    assertWebMessage(
        "OK",
        200,
        "OK",
        "All keys from namespace 'test' deleted.",
        DELETE("/userDataStore/test?username=Paul").content(HttpStatus.OK));

    switchContextToUser(paul);
    assertEquals(HttpStatus.NOT_FOUND, GET("/userDataStore/test").status());
  }

  @Test
  void testDeleteKeys_NotSuperuser() {
    assertStatus(HttpStatus.CREATED, POST("/userDataStore/test/key1", "true"));

    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        "Only superusers can read or write other users data using the `user` parameter.",
        DELETE("/userDataStore/test?username=Paul").content(HttpStatus.CONFLICT));
  }

  @Test
  void testAddUserKeyJsonValue() {
    switchToSuperuser();
    assertWebMessage(
        "Created",
        201,
        "OK",
        "Key 'key1' in namespace 'test' created.",
        POST("/userDataStore/test/key1?username=Paul", "true").content(HttpStatus.CREATED));

    switchContextToUser(paul);
    assertTrue(
        GET("/userDataStore/test/key1")
            .content(HttpStatus.OK)
            .as(JsonBoolean.class)
            .booleanValue());
  }

  @Test
  void testAddUserKeyJsonValue_MalformedValue() {
    switchToSuperuser();
    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "The data is not valid JSON.",
        POST("/userDataStore/test/key1?username=Paul", "invalidJson")
            .content(HttpStatus.BAD_REQUEST)
            .as(JsonWebMessage.class));
  }

  @Test
  void testAddUserKeyJsonValue_AlreadyExists() {
    assertStatus(HttpStatus.CREATED, POST("/userDataStore/test/key1", "true"));

    switchToSuperuser();
    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        "The key 'key1' already exists in the namespace 'test'.",
        POST("/userDataStore/test/key1?username=Paul", "true")
            .content(HttpStatus.CONFLICT)
            .as(JsonWebMessage.class));
  }

  @Test
  void testAddUserKeyJsonValue_NotSuperuser() {
    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        "Only superusers can read or write other users data using the `user` parameter.",
        POST("/userDataStore/test/key1?username=Paul", "true")
            .content(HttpStatus.CONFLICT)
            .as(JsonWebMessage.class));
  }

  @Test
  void testUpdateUserKeyJsonValue() {
    assertStatus(HttpStatus.CREATED, POST("/userDataStore/test/key1", "true"));

    switchToSuperuser();
    assertWebMessage(
        "Created",
        201,
        "OK",
        "Key 'key1' in namespace 'test' updated.",
        PUT("/userDataStore/test/key1?username=Paul", "false").content(HttpStatus.CREATED));

    switchContextToUser(paul);
    assertFalse(
        GET("/userDataStore/test/key1")
            .content(HttpStatus.OK)
            .as(JsonBoolean.class)
            .booleanValue());
  }

  @Test
  void testUpdateUserKeyJsonValue_UnknownKey() {
    switchToSuperuser();
    assertWebMessage(
        "Not Found",
        404,
        "ERROR",
        "The key 'unknown' was not found in the namespace 'test'.",
        PUT("/userDataStore/test/unknown?username=Paul", "false").content(HttpStatus.NOT_FOUND));
  }

  @Test
  void testUpdateUserKeyJsonValue_MalformedValue() {
    assertStatus(HttpStatus.CREATED, POST("/userDataStore/test/key1", "true"));

    switchToSuperuser();
    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "The data is not valid JSON.",
        PUT("/userDataStore/test/key1?username=Paul", "invalidJson")
            .error(HttpStatus.BAD_REQUEST)
            .as(JsonWebMessage.class));
  }

  @Test
  void testUpdateUserKeyJsonValue_NotSuperuser() {
    assertStatus(HttpStatus.CREATED, POST("/userDataStore/test/key1", "true"));

    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        "Only superusers can read or write other users data using the `user` parameter.",
        PUT("/userDataStore/test/key1?username=Paul", "false")
            .error(HttpStatus.CONFLICT)
            .as(JsonWebMessage.class));
  }

  @Test
  void testDeleteUserKeyJsonValue() {
    assertStatus(HttpStatus.CREATED, POST("/userDataStore/test/key1", "true"));
    assertStatus(HttpStatus.CREATED, POST("/userDataStore/test/key2", "42"));

    switchToSuperuser();
    assertWebMessage(
        "OK",
        200,
        "OK",
        "Key 'key1' deleted from the namespace 'test'.",
        DELETE("/userDataStore/test/key1?username=Paul").content(HttpStatus.OK));

    switchContextToUser(paul);
    assertEquals(List.of("key2"), GET("/userDataStore/test").content().stringValues());
  }

  @Test
  void testDeleteUserKeyJsonValue_UnknownKey() {
    switchToSuperuser();
    assertWebMessage(
        "Not Found",
        404,
        "ERROR",
        "The key 'key1' was not found in the namespace 'test'.",
        DELETE("/userDataStore/test/key1?username=Paul").content(HttpStatus.NOT_FOUND));
  }

  @Test
  void testDeleteUserKeyJsonValue_NotSuperuser() {
    assertStatus(HttpStatus.CREATED, POST("/userDataStore/test/key1", "true"));

    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        "Only superusers can read or write other users data using the `user` parameter.",
        DELETE("/userDataStore/test/key1?username=Paul").content(HttpStatus.CONFLICT));
  }

  @Test
  void testGetNamespaces() {
    assertStatus(HttpStatus.CREATED, POST("/userDataStore/test/key1", "true"));
    assertStatus(HttpStatus.CREATED, POST("/userDataStore/test2/key1", "42"));

    switchToSuperuser();
    assertContainsOnly(
        List.of("test", "test2"), GET("/userDataStore?username=Paul").content().stringValues());
  }
}
