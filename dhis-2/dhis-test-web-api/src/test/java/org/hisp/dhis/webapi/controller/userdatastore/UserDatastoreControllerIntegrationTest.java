/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi.controller.userdatastore;

import static java.util.Arrays.asList;
import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerIntegrationTest;
import org.hisp.dhis.webapi.controller.UserDatastoreController;
import org.hisp.dhis.webapi.json.domain.JsonDatastoreValue;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link UserDatastoreController} using (mocked) REST requests.
 *
 * @author david mackessy
 */
class UserDatastoreControllerIntegrationTest extends DhisControllerIntegrationTest {

  @Test
  void testPutEntry_EntryAlreadyExistsAndIsUpdated() {
    doInTransaction(
        () ->
            assertStatus(
                HttpStatus.CREATED, PUT("/userDataStore/pets/emu", "{\"name\":\"harry\"}")));
    doInTransaction(
        () -> assertStatus(HttpStatus.OK, PUT("/userDataStore/pets/emu", "{\"name\":\"james\"}")));
    JsonDatastoreValue emu = GET("/userDataStore/pets/emu").content().as(JsonDatastoreValue.class);
    assertEquals("james", emu.getString("name").string());
  }

  @Test
  void testUpdateKeyJsonValue() {
    doInTransaction(
        () -> assertStatus(HttpStatus.CREATED, POST("/userDataStore/animals/cat", "[]")));
    doInTransaction(
        () -> assertStatus(HttpStatus.OK, PUT("/userDataStore/animals/cat", "[1,2,3]")));
    assertEquals(asList(1, 2, 3), GET("/userDataStore/animals/cat").content().numberValues());
  }

  @Test
  void testUpdateUserKeyJsonValue() {
    assertStatus(HttpStatus.CREATED, POST("/userDataStore/test/key1", "true"));
    assertWebMessage(
        "OK",
        200,
        "OK",
        "Key updated: 'key1'",
        PUT("/userDataStore/test/key1", "false").content(HttpStatus.OK));
  }
}
