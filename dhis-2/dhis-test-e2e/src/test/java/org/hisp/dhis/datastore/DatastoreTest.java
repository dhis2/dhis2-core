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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonArray;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.actions.UserActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author david mackessy
 */
class DatastoreTest extends ApiTest {

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
  void testDatastoreSharing_SuperUser() {
    // add 2 entries as admin
    loginActions.loginAsAdmin();
    String key1 = "arsenal";
    String key2 = "spurs";
    datastoreActions.post("/" + NAMESPACE + "/" + key1, newEntry(key1)).validate().statusCode(201);
    datastoreActions.post("/" + NAMESPACE + "/" + key2, newEntry(key2)).validate().statusCode(201);

    // make call with fields query param as super user and check can see 2 entries
    loginActions.loginAsSuperUser();
    QueryParamsBuilder params = new QueryParamsBuilder().add("fields", "league");
    ApiResponse getResponse = datastoreActions.get("/" + NAMESPACE, params).validateStatus(200);

    JsonArray entries = getResponse.getBody().getAsJsonArray("entries");
    assertEquals("{\"key\":\"arsenal\",\"league\":\"prem\"}", entries.get(0).toString());
    assertEquals("{\"key\":\"spurs\",\"league\":\"prem\"}", entries.get(1).toString());
    assertEquals(2, entries.size());
  }

  @Test
  void testDatastoreUserSharing_UserNoAccess() {
    // add 2 entries as admin
    loginActions.loginAsAdmin();
    String key1 = "arsenal";
    String key2 = "spurs";
    datastoreActions.post("/" + NAMESPACE + "/" + key1, newEntry(key1)).validate().statusCode(201);
    datastoreActions.post("/" + NAMESPACE + "/" + key2, newEntry(key2)).validate().statusCode(201);

    // make call with fields query param as user with no access and check can see no entries
    loginActions.loginAsUser(BASIC_USER, "Test1234!");

    QueryParamsBuilder params = new QueryParamsBuilder().add("fields", "league");
    ApiResponse getResponse = datastoreActions.get("/" + NAMESPACE, params).validateStatus(200);

    JsonArray entries = getResponse.getBody().getAsJsonArray("entries");
    assertEquals(0, entries.size());
  }

  @Test
  void testDatastoreUserSharing_UserHasAccess() {
    // add 2 entries as admin
    loginActions.loginAsAdmin();
    String key1 = "arsenal";
    String key2 = "spurs";
    datastoreActions.post("/" + NAMESPACE + "/" + key1, newEntry(key1)).validate().statusCode(201);
    datastoreActions.post("/" + NAMESPACE + "/" + key2, newEntry(key2)).validate().statusCode(201);

    // get ids of entries
    ApiResponse mdResponse1 = datastoreActions.get("/" + NAMESPACE + "/" + key1 + "/metaData");
    String uid1 = mdResponse1.extractUid();
    ApiResponse mdResponse2 = datastoreActions.get("/" + NAMESPACE + "/" + key2 + "/metaData");
    String uid2 = mdResponse2.extractUid();

    // share entries with user
    QueryParamsBuilder params = new QueryParamsBuilder().add("type", "dataStore").add("id", uid1);
    sharingActions.post("", sharingUserAccess(basicUserId), params).validateStatus(200);
    QueryParamsBuilder params2 = new QueryParamsBuilder().add("type", "dataStore").add("id", uid2);
    sharingActions.post("", sharingUserAccess(basicUserId), params2).validateStatus(200);

    // make call with fields query param as user with access and check can see entries
    loginActions.loginAsUser(BASIC_USER, "Test1234!");

    QueryParamsBuilder fieldsParam = new QueryParamsBuilder().add("fields", "league");
    ApiResponse getResponse = datastoreActions.get("/" + NAMESPACE, fieldsParam);

    JsonArray entries = getResponse.getBody().getAsJsonArray("entries");
    assertEquals("{\"key\":\"arsenal\",\"league\":\"prem\"}", entries.get(0).toString());
    assertEquals("{\"key\":\"spurs\",\"league\":\"prem\"}", entries.get(1).toString());
    assertEquals(2, entries.size());
  }

  @Test
  void testDatastoreUserSharing_UserHasSomeAccess() {
    // add 2 entries as admin
    loginActions.loginAsAdmin();
    String key1 = "arsenal";
    String key2 = "spurs";
    datastoreActions.post("/" + NAMESPACE + "/" + key1, newEntry(key1)).validate().statusCode(201);
    datastoreActions.post("/" + NAMESPACE + "/" + key2, newEntry(key2)).validate().statusCode(201);

    // get id of 1 entry
    ApiResponse mdResponse =
        datastoreActions.get("/" + NAMESPACE + "/" + key2 + "/metaData").validateStatus(200);
    String uid = mdResponse.extractUid();

    // share entry with user
    QueryParamsBuilder params = new QueryParamsBuilder().add("type", "dataStore").add("id", uid);
    sharingActions.post("", sharingUserAccess(basicUserId), params).validateStatus(200);

    // make call with fields query as user with some access and check can see 1 entry
    loginActions.loginAsUser(BASIC_USER, "Test1234!");

    QueryParamsBuilder fieldsParam = new QueryParamsBuilder().add("fields", "league");
    ApiResponse getResponse =
        datastoreActions.get("/" + NAMESPACE, fieldsParam).validateStatus(200);

    JsonArray entries = getResponse.getBody().getAsJsonArray("entries");
    assertEquals("{\"key\":\"spurs\",\"league\":\"prem\"}", entries.get(0).toString());
    assertEquals(1, entries.size());
  }

  @Test
  void testDatastoreUserGroupSharing_UserHasAccess() {
    // add 2 entries as admin
    loginActions.loginAsAdmin();
    String key1 = "arsenal";
    String key2 = "spurs";
    datastoreActions.post("/" + NAMESPACE + "/" + key1, newEntry(key1)).validate().statusCode(201);
    datastoreActions.post("/" + NAMESPACE + "/" + key2, newEntry(key2)).validate().statusCode(201);

    // get ids of entries
    ApiResponse mdResponse1 = datastoreActions.get("/" + NAMESPACE + "/" + key1 + "/metaData");
    String uid1 = mdResponse1.extractUid();
    ApiResponse mdResponse2 = datastoreActions.get("/" + NAMESPACE + "/" + key2 + "/metaData");
    String uid2 = mdResponse2.extractUid();

    // add user to user group
    userActions.post(basicUserId + "/userGroups/" + userGroupId, "").validateStatus(200);

    // share entries with user group
    QueryParamsBuilder params = new QueryParamsBuilder().add("type", "dataStore").add("id", uid1);
    sharingActions.post("", sharingUserGroupAccess(userGroupId), params).validateStatus(200);
    QueryParamsBuilder params2 = new QueryParamsBuilder().add("type", "dataStore").add("id", uid2);
    sharingActions.post("", sharingUserGroupAccess(userGroupId), params2).validateStatus(200);

    // make call with fields query as user with access and check can see all entries
    loginActions.loginAsUser(BASIC_USER, "Test1234!");

    QueryParamsBuilder fieldsParam = new QueryParamsBuilder().add("fields", "league");
    ApiResponse getResponse =
        datastoreActions.get("/" + NAMESPACE, fieldsParam).validateStatus(200);

    JsonArray entries = getResponse.getBody().getAsJsonArray("entries");
    assertEquals("{\"key\":\"arsenal\",\"league\":\"prem\"}", entries.get(0).toString());
    assertEquals("{\"key\":\"spurs\",\"league\":\"prem\"}", entries.get(1).toString());
    assertEquals(2, entries.size());
  }

  @Test
  void testDatastoreUserGroupSharing_UserHasSomeAccess() {
    // add 2 entries as admin
    loginActions.loginAsAdmin();
    String key1 = "arsenal";
    String key2 = "spurs";
    datastoreActions.post("/" + NAMESPACE + "/" + key1, newEntry(key1)).validate().statusCode(201);
    datastoreActions.post("/" + NAMESPACE + "/" + key2, newEntry(key2)).validate().statusCode(201);

    // get id of 1 entry
    ApiResponse mdResponse = datastoreActions.get("/" + NAMESPACE + "/" + key1 + "/metaData");
    String uid = mdResponse.extractUid();

    // add user to user group
    userActions.post(basicUserId + "/userGroups/" + userGroupId, "").validateStatus(200);

    // give access to user for 1 entry only
    QueryParamsBuilder params = new QueryParamsBuilder().add("type", "dataStore").add("id", uid);
    sharingActions.post("", sharingUserGroupAccess(userGroupId), params).validateStatus(200);

    // make call with fields query as user with some access and check can see 1 entry
    loginActions.loginAsUser(BASIC_USER, "Test1234!");

    QueryParamsBuilder fieldsParam = new QueryParamsBuilder().add("fields", "league");
    ApiResponse getResponse =
        datastoreActions.get("/" + NAMESPACE, fieldsParam).validateStatus(200);

    JsonArray entries = getResponse.getBody().getAsJsonArray("entries");
    assertEquals("{\"key\":\"arsenal\",\"league\":\"prem\"}", entries.get(0).toString());
    assertEquals(1, entries.size());
  }

  private String newEntry(String team) {
    return """
      {"name": "%s","league": "prem"}
    """.strip().formatted(team);
  }

  private String sharingUserAccess(String userId) {
    return """
    {
        "object": {
            "publicAccess": "--r-----",
            "externalAccess": false,
            "user": {},
            "userAccesses": [
                {
                    "id": "%s",
                    "access": "--r-----"
                }
            ],
            "userGroupAccesses": []
        }
    }
    """
        .formatted(userId)
        .strip();
  }

  private String sharingUserGroupAccess(String userGroupId) {
    return """
    {
        "object": {
            "publicAccess": "--------",
            "externalAccess": false,
            "user": {},
            "userAccesses": [],
            "userGroupAccesses": [
                {
                    "id": "%s",
                    "access": "--r-----"
                }
            ]
        }
    }
    """
        .formatted(userGroupId)
        .strip();
  }
}
