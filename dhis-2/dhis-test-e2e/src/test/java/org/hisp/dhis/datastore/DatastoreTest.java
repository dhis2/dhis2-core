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

  private RestApiActions datastoreApiActions;
  private RestApiActions sharingActions;
  private LoginActions loginActions;
  private UserActions userActions;

  private static final String NAMESPACE = "football";
  private static final String BASIC_USER = "User123";
  private String basicUserId = "";
  private String userGroupId = "";

  @BeforeAll
  public void beforeAll() {
    datastoreApiActions = new RestApiActions("dataStore");
    sharingActions = new RestApiActions("sharing");
    loginActions = new LoginActions();
    userActions = new UserActions();
    basicUserId = userActions.addUser(BASIC_USER, "Test1234!");

    RestApiActions userGroupActions = new RestApiActions("userGroups");
    userGroupId = userGroupActions.post("{\"name\":\"basic user group\"}").extractUid();
  }

  @AfterEach
  public void deleteEntries() {
    datastoreApiActions.delete(NAMESPACE).validateStatus(200);
  }

  @Test
  void testDatastoreSharing_SuperUser() {
    // put 2 entries into namespace
    loginActions.loginAsAdmin();

    // add 2 entries as admin
    String key1 = "arsenal";
    String key2 = "spurs";
    datastoreApiActions
        .post("/" + NAMESPACE + "/" + key1, newEntry(key1))
        .validate()
        .statusCode(201);
    datastoreApiActions
        .post("/" + NAMESPACE + "/" + key2, newEntry(key2))
        .validate()
        .statusCode(201);

    // make call with fields query as super user and check can see 2 entries
    loginActions.loginAsSuperUser();
    QueryParamsBuilder paramsBuilder2 = new QueryParamsBuilder().add("fields", "league");
    ApiResponse response2 = datastoreApiActions.get("/" + NAMESPACE, paramsBuilder2);

    JsonArray entries2 = response2.getBody().getAsJsonArray("entries");
    assertEquals("{\"key\":\"arsenal\",\"league\":\"prem\"}", entries2.get(0).toString());
    assertEquals("{\"key\":\"spurs\",\"league\":\"prem\"}", entries2.get(1).toString());
    assertEquals(2, entries2.size());
  }

  @Test
  void testDatastoreUserSharing_UserNoAccess() {
    // put 2 entries into namespace
    loginActions.loginAsAdmin();

    // add 2 entries as admin
    String key1 = "arsenal";
    String key2 = "spurs";
    datastoreApiActions
        .post("/" + NAMESPACE + "/" + key1, newEntry(key1))
        .validate()
        .statusCode(201);
    datastoreApiActions
        .post("/" + NAMESPACE + "/" + key2, newEntry(key2))
        .validate()
        .statusCode(201);

    // make call with fields query as user with no access and check can see no entries
    loginActions.loginAsUser(BASIC_USER, "Test1234!");

    QueryParamsBuilder paramsBuilder2 = new QueryParamsBuilder().add("fields", "league");
    ApiResponse response2 = datastoreApiActions.get("/" + NAMESPACE, paramsBuilder2);

    JsonArray entries2 = response2.getBody().getAsJsonArray("entries");
    assertEquals(0, entries2.size());
  }

  @Test
  void testDatastoreUserSharing_UserHasAccess() {
    // put 2 entries into namespace
    loginActions.loginAsAdmin();

    // add 2 entries as admin
    String key1 = "arsenal";
    String key2 = "spurs";
    datastoreApiActions
        .post("/" + NAMESPACE + "/" + key1, newEntry(key1))
        .validate()
        .statusCode(201);
    datastoreApiActions
        .post("/" + NAMESPACE + "/" + key2, newEntry(key2))
        .validate()
        .statusCode(201);

    // get ids of entries
    ApiResponse mdResponse1 = datastoreApiActions.get("/" + NAMESPACE + "/" + key1 + "/metaData");
    String uid1 = mdResponse1.extractUid();
    ApiResponse mdResponse2 = datastoreApiActions.get("/" + NAMESPACE + "/" + key2 + "/metaData");
    String uid2 = mdResponse2.extractUid();

    // give access to user
    QueryParamsBuilder params = new QueryParamsBuilder().add("type", "dataStore").add("id", uid1);
    sharingActions.post("", sharingUserAccess(basicUserId), params).validateStatus(200);
    QueryParamsBuilder params2 = new QueryParamsBuilder().add("type", "dataStore").add("id", uid2);
    sharingActions.post("", sharingUserAccess(basicUserId), params2).validateStatus(200);

    // make call with fields query as user with no access and check can see no entries
    loginActions.loginAsUser(BASIC_USER, "Test1234!");

    QueryParamsBuilder paramsBuilder2 = new QueryParamsBuilder().add("fields", "league");
    ApiResponse response2 = datastoreApiActions.get("/" + NAMESPACE, paramsBuilder2);

    JsonArray entries2 = response2.getBody().getAsJsonArray("entries");
    assertEquals("{\"key\":\"arsenal\",\"league\":\"prem\"}", entries2.get(0).toString());
    assertEquals("{\"key\":\"spurs\",\"league\":\"prem\"}", entries2.get(1).toString());
    assertEquals(2, entries2.size());
  }

  @Test
  void testDatastoreUserSharing_UserHasSomeAccess() {
    // put 2 entries into namespace
    loginActions.loginAsAdmin();

    // add 2 entries as admin
    String key1 = "arsenal";
    String key2 = "spurs";
    datastoreApiActions
        .post("/" + NAMESPACE + "/" + key1, newEntry(key1))
        .validate()
        .statusCode(201);
    datastoreApiActions
        .post("/" + NAMESPACE + "/" + key2, newEntry(key2))
        .validate()
        .statusCode(201);

    // get id of 1 entry
    ApiResponse mdResponse1 = datastoreApiActions.get("/" + NAMESPACE + "/" + key2 + "/metaData");
    String uid1 = mdResponse1.extractUid();

    // give access to user
    QueryParamsBuilder params = new QueryParamsBuilder().add("type", "dataStore").add("id", uid1);
    sharingActions.post("", sharingUserAccess(basicUserId), params).validateStatus(200);

    // make call with fields query as user with no access and check can see no entries
    loginActions.loginAsUser(BASIC_USER, "Test1234!");

    QueryParamsBuilder paramsBuilder2 = new QueryParamsBuilder().add("fields", "league");
    ApiResponse response2 = datastoreApiActions.get("/" + NAMESPACE, paramsBuilder2);

    JsonArray entries2 = response2.getBody().getAsJsonArray("entries");
    assertEquals("{\"key\":\"spurs\",\"league\":\"prem\"}", entries2.get(0).toString());
    assertEquals(1, entries2.size());
  }

  @Test
  void testDatastoreUserGroupSharing_UserHasAccess() {
    // put 2 entries into namespace
    loginActions.loginAsAdmin();

    // add 2 entries as admin
    String key1 = "arsenal";
    String key2 = "spurs";
    datastoreApiActions
        .post("/" + NAMESPACE + "/" + key1, newEntry(key1))
        .validate()
        .statusCode(201);
    datastoreApiActions
        .post("/" + NAMESPACE + "/" + key2, newEntry(key2))
        .validate()
        .statusCode(201);

    // get ids of entries
    ApiResponse mdResponse1 = datastoreApiActions.get("/" + NAMESPACE + "/" + key1 + "/metaData");
    String uid1 = mdResponse1.extractUid();
    ApiResponse mdResponse2 = datastoreApiActions.get("/" + NAMESPACE + "/" + key2 + "/metaData");
    String uid2 = mdResponse2.extractUid();

    // add user to user group
    userActions.post(basicUserId + "/userGroups/" + userGroupId, "").validateStatus(200);

    // give access to user for both entries
    QueryParamsBuilder params = new QueryParamsBuilder().add("type", "dataStore").add("id", uid1);
    sharingActions.post("", sharingUserGroupAccess(userGroupId), params).validateStatus(200);
    QueryParamsBuilder params2 = new QueryParamsBuilder().add("type", "dataStore").add("id", uid2);
    sharingActions.post("", sharingUserGroupAccess(userGroupId), params2).validateStatus(200);

    // make call with fields query as user with no access and check can see no entries
    loginActions.loginAsUser(BASIC_USER, "Test1234!");

    QueryParamsBuilder paramsBuilder2 = new QueryParamsBuilder().add("fields", "league");
    ApiResponse response2 = datastoreApiActions.get("/" + NAMESPACE, paramsBuilder2);

    JsonArray entries2 = response2.getBody().getAsJsonArray("entries");
    assertEquals("{\"key\":\"arsenal\",\"league\":\"prem\"}", entries2.get(0).toString());
    assertEquals("{\"key\":\"spurs\",\"league\":\"prem\"}", entries2.get(1).toString());
    assertEquals(2, entries2.size());
  }

  @Test
  void testDatastoreUserGroupSharing_UserHasSomeAccess() {
    // put 2 entries into namespace
    loginActions.loginAsAdmin();

    // add 2 entries as admin
    String key1 = "arsenal";
    String key2 = "spurs";
    datastoreApiActions
        .post("/" + NAMESPACE + "/" + key1, newEntry(key1))
        .validate()
        .statusCode(201);
    datastoreApiActions
        .post("/" + NAMESPACE + "/" + key2, newEntry(key2))
        .validate()
        .statusCode(201);

    // get ids of entries
    ApiResponse mdResponse1 = datastoreApiActions.get("/" + NAMESPACE + "/" + key1 + "/metaData");
    String uid1 = mdResponse1.extractUid();

    // add user to user group
    userActions.post(basicUserId + "/userGroups/" + userGroupId, "").validateStatus(200);

    // give access to user for 1 entry only
    QueryParamsBuilder params = new QueryParamsBuilder().add("type", "dataStore").add("id", uid1);
    sharingActions.post("", sharingUserGroupAccess(userGroupId), params).validateStatus(200);

    // make call with fields query as user with no access and check can see no entries
    loginActions.loginAsUser(BASIC_USER, "Test1234!");

    QueryParamsBuilder paramsBuilder2 = new QueryParamsBuilder().add("fields", "league");
    ApiResponse response2 = datastoreApiActions.get("/" + NAMESPACE, paramsBuilder2);

    JsonArray entries2 = response2.getBody().getAsJsonArray("entries");
    assertEquals("{\"key\":\"arsenal\",\"league\":\"prem\"}", entries2.get(0).toString());
    assertEquals(1, entries2.size());
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
        .formatted(userId);
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
        .formatted(userGroupId);
  }
}
