/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.datastore;

import static org.hisp.dhis.datastore.DatastoreKeysTest.newEntry;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.actions.UserActions;
import org.hisp.dhis.dto.ApiResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author david mackessy
 */
class DatastoreEntriesTest extends ApiTest {

  private RestApiActions datastoreActions;
  private RestApiActions sharingActions;
  private LoginActions loginActions;
  private UserActions userActions;

  private static final String NAMESPACE = "football";
  private static final String BASIC_USER = "User123";
  private String basicUserId = "";
  private String userGroupId = "";

  @BeforeAll
  public void beforeAll() {
    datastoreActions = new RestApiActions("dataStore");
    sharingActions = new RestApiActions("sharing");
    loginActions = new LoginActions();
    userActions = new UserActions();
    basicUserId = userActions.addUser(BASIC_USER, "Test1234!");

    RestApiActions userGroupActions = new RestApiActions("userGroups");
    userGroupId = userGroupActions.post("{\"name\":\"basic user group\"}").extractUid();
  }

  @AfterEach
  public void deleteEntries() {
    datastoreActions.delete(NAMESPACE).validateStatus(200);
  }

  @Test
  @DisplayName("Basic user can read a datastore entry with default sharing")
  void testDatastoreSharing_DefaultPublicAccess_BasicUser() {
    // add 2 entries as admin
    loginActions.loginAsAdmin();
    String key1 = "arsenal";
    datastoreActions.post("/" + NAMESPACE + "/" + key1, newEntry(key1)).validate().statusCode(201);

    // make call with fields query param as basic user and check can see 2 entries
    loginActions.loginAsUser(BASIC_USER, "Test1234!");
    ApiResponse getResponse =
        datastoreActions.get("/" + NAMESPACE + "/" + key1).validateStatus(200);
    assertEquals("{\"name\": \"arsenal\", \"league\": \"prem\"}", getResponse.getAsString());
  }
}
