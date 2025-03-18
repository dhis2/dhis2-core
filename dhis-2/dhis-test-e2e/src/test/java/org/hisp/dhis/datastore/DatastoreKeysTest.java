/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.datastore;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

import com.google.gson.JsonObject;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.test.e2e.actions.LoginActions;
import org.hisp.dhis.test.e2e.actions.RestApiActions;
import org.hisp.dhis.test.e2e.actions.UserActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author david mackessy
 */
class DatastoreKeysTest extends ApiTest {

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
    loginActions.loginAsSuperUser();
    datastoreActions
        .post("/" + NAMESPACE + "/testEntry", getEntry("testEntry"))
        .validate()
        .statusCode(201);
    datastoreActions.delete(NAMESPACE).validateStatus(200);
  }

  @Test
  void testDatastoreSharing_DefaultPublicAccess_BasicUser() {
    // add 2 entries as admin
    loginActions.loginAsAdmin();
    String key1 = "arsenal";
    String key2 = "spurs";
    datastoreActions.post("/" + NAMESPACE + "/" + key1, getEntry(key1)).validate().statusCode(201);
    datastoreActions.post("/" + NAMESPACE + "/" + key2, getEntry(key2)).validate().statusCode(201);

    // make call with fields query param as basic user and check can see 2 entries
    loginActions.loginAsUser(BASIC_USER, "Test1234!");
    QueryParamsBuilder params = new QueryParamsBuilder().add("fields", "league");
    datastoreActions
        .get("/" + NAMESPACE, params)
        .validate()
        .statusCode(200)
        .body("entries.key", allOf(hasItem("arsenal"), hasItem("spurs")));
  }

  @Test
  void testDatastoreSharing_NoPublicAccess_SuperUser() {
    // add 2 entries as admin
    loginActions.loginAsAdmin();
    String key1 = "arsenal";
    String key2 = "spurs";
    datastoreActions.post("/" + NAMESPACE + "/" + key1, getEntry(key1)).validate().statusCode(201);
    datastoreActions.post("/" + NAMESPACE + "/" + key2, getEntry(key2)).validate().statusCode(201);

    // get ids of entries
    ApiResponse mdResponse1 = datastoreActions.get("/" + NAMESPACE + "/" + key1 + "/metaData");
    String uid1 = mdResponse1.extractUid();

    // set sharing public access to '--------'
    QueryParamsBuilder sharingParams =
        new QueryParamsBuilder().add("type", "dataStore").add("id", uid1);
    sharingActions.post("", sharingNoPublicAccess(), sharingParams).validateStatus(200);

    // make call with fields query param as superuser and check can see 2 entries
    loginActions.loginAsSuperUser();
    QueryParamsBuilder params = new QueryParamsBuilder().add("fields", "league");
    datastoreActions
        .get("/" + NAMESPACE, params)
        .validate()
        .statusCode(200)
        .body("entries.key", allOf(hasItem("arsenal"), hasItem("spurs")));
  }

  @Test
  void testDatastoreUserSharing_NoPublicAccess_UserNoAccess() {
    // add 2 entries as admin
    loginActions.loginAsAdmin();
    String key1 = "arsenal";
    String key2 = "spurs";
    datastoreActions.post("/" + NAMESPACE + "/" + key1, getEntry(key1)).validate().statusCode(201);
    datastoreActions.post("/" + NAMESPACE + "/" + key2, getEntry(key2)).validate().statusCode(201);

    // get ids of entries
    ApiResponse mdResponse1 = datastoreActions.get("/" + NAMESPACE + "/" + key1 + "/metaData");
    String uid1 = mdResponse1.extractUid();
    ApiResponse mdResponse2 = datastoreActions.get("/" + NAMESPACE + "/" + key2 + "/metaData");
    String uid2 = mdResponse2.extractUid();

    // set sharing public access to '--------'
    QueryParamsBuilder sharingParams1 =
        new QueryParamsBuilder().add("type", "dataStore").add("id", uid1);
    sharingActions.post("", sharingNoPublicAccess(), sharingParams1).validateStatus(200);
    QueryParamsBuilder sharingParams2 =
        new QueryParamsBuilder().add("type", "dataStore").add("id", uid2);
    sharingActions.post("", sharingNoPublicAccess(), sharingParams2).validateStatus(200);

    // make call with fields query param as user with no access and check can see no entries
    loginActions.loginAsUser(BASIC_USER, "Test1234!");
    QueryParamsBuilder params = new QueryParamsBuilder().add("fields", "league");
    datastoreActions
        .get("/" + NAMESPACE, params)
        .validate()
        .statusCode(200)
        .body("entries.size()", equalTo(0));
  }

  @Test
  void testDatastoreUserSharing_NoPublicAccess_UserHasAccess() {
    // add 2 entries as admin
    loginActions.loginAsAdmin();
    String key1 = "arsenal";
    String key2 = "spurs";
    datastoreActions.post("/" + NAMESPACE + "/" + key1, getEntry(key1)).validate().statusCode(201);
    datastoreActions.post("/" + NAMESPACE + "/" + key2, getEntry(key2)).validate().statusCode(201);

    // get ids of entries
    ApiResponse mdResponse1 = datastoreActions.get("/" + NAMESPACE + "/" + key1 + "/metaData");
    String uid1 = mdResponse1.extractUid();
    ApiResponse mdResponse2 = datastoreActions.get("/" + NAMESPACE + "/" + key2 + "/metaData");
    String uid2 = mdResponse2.extractUid();

    // share entries with user and set public access to '--------'
    QueryParamsBuilder params = new QueryParamsBuilder().add("type", "dataStore").add("id", uid1);
    sharingActions.post("", sharingUserAccess(basicUserId), params).validateStatus(200);
    QueryParamsBuilder params2 = new QueryParamsBuilder().add("type", "dataStore").add("id", uid2);
    sharingActions.post("", sharingUserAccess(basicUserId), params2).validateStatus(200);

    // make call with fields query param as user with access and check can see entries
    loginActions.loginAsUser(BASIC_USER, "Test1234!");

    QueryParamsBuilder fieldsParam = new QueryParamsBuilder().add("fields", "league");
    datastoreActions
        .get("/" + NAMESPACE, fieldsParam)
        .validate()
        .statusCode(200)
        .body("entries.key", allOf(hasItem("arsenal"), hasItem("spurs")));
  }

  @Test
  void testDatastoreUserSharing_NoPublicAccess_UserHasSomeAccess() {
    // add 2 entries as admin
    loginActions.loginAsAdmin();
    String key1 = "arsenal";
    String key2 = "spurs";
    datastoreActions.post("/" + NAMESPACE + "/" + key1, getEntry(key1)).validate().statusCode(201);
    datastoreActions.post("/" + NAMESPACE + "/" + key2, getEntry(key2)).validate().statusCode(201);

    // get id of 1 entry
    ApiResponse mdResponse1 =
        datastoreActions.get("/" + NAMESPACE + "/" + key1 + "/metaData").validateStatus(200);
    String uid1 = mdResponse1.extractUid();
    ApiResponse mdResponse2 =
        datastoreActions.get("/" + NAMESPACE + "/" + key2 + "/metaData").validateStatus(200);
    String uid2 = mdResponse2.extractUid();

    // set no public access sharing on 1 entry
    QueryParamsBuilder sharingParams1 =
        new QueryParamsBuilder().add("type", "dataStore").add("id", uid1);
    sharingActions.post("", sharingNoPublicAccess(), sharingParams1).validateStatus(200);

    // share other entry with user and set public access to '--------'
    QueryParamsBuilder params = new QueryParamsBuilder().add("type", "dataStore").add("id", uid2);
    sharingActions.post("", sharingUserAccess(basicUserId), params).validateStatus(200);

    // make call with fields query as user with some access and check can see 1 entry
    loginActions.loginAsUser(BASIC_USER, "Test1234!");

    QueryParamsBuilder fieldsParam = new QueryParamsBuilder().add("fields", "league");
    datastoreActions
        .get("/" + NAMESPACE, fieldsParam)
        .validate()
        .statusCode(200)
        .body("entries.key", allOf(hasItem("spurs")))
        .body("entries.size()", equalTo(1));
  }

  @Test
  void testDatastoreUserGroupSharing_NoPublicAccess_UserHasAccess() {
    // add 2 entries as admin
    loginActions.loginAsAdmin();
    String key1 = "arsenal";
    String key2 = "spurs";
    datastoreActions.post("/" + NAMESPACE + "/" + key1, getEntry(key1)).validate().statusCode(201);
    datastoreActions.post("/" + NAMESPACE + "/" + key2, getEntry(key2)).validate().statusCode(201);

    // get ids of entries
    ApiResponse mdResponse1 = datastoreActions.get("/" + NAMESPACE + "/" + key1 + "/metaData");
    String uid1 = mdResponse1.extractUid();
    ApiResponse mdResponse2 = datastoreActions.get("/" + NAMESPACE + "/" + key2 + "/metaData");
    String uid2 = mdResponse2.extractUid();

    // add user to user group
    userActions.post(basicUserId + "/userGroups/" + userGroupId, "").validateStatus(200);

    // share entries with user group and set public access to '--------'
    QueryParamsBuilder params = new QueryParamsBuilder().add("type", "dataStore").add("id", uid1);
    sharingActions.post("", sharingUserGroupAccess(userGroupId), params).validateStatus(200);
    QueryParamsBuilder params2 = new QueryParamsBuilder().add("type", "dataStore").add("id", uid2);
    sharingActions.post("", sharingUserGroupAccess(userGroupId), params2).validateStatus(200);

    // make call with fields query as user with access and check can see all entries
    loginActions.loginAsUser(BASIC_USER, "Test1234!");

    QueryParamsBuilder fieldsParam = new QueryParamsBuilder().add("fields", "league");
    datastoreActions
        .get("/" + NAMESPACE, fieldsParam)
        .validate()
        .statusCode(200)
        .body("entries.key", allOf(hasItem("arsenal"), hasItem("spurs")));
  }

  @Test
  void testDatastoreUserGroupSharing_NoPublicAccess_UserHasSomeAccess() {
    // add 2 entries as admin
    loginActions.loginAsAdmin();
    String key1 = "arsenal";
    String key2 = "spurs";
    datastoreActions.post("/" + NAMESPACE + "/" + key1, getEntry(key1)).validate().statusCode(201);
    datastoreActions.post("/" + NAMESPACE + "/" + key2, getEntry(key2)).validate().statusCode(201);

    // get id of 1 entry
    ApiResponse mdResponse1 = datastoreActions.get("/" + NAMESPACE + "/" + key1 + "/metaData");
    String uid1 = mdResponse1.extractUid();
    ApiResponse mdResponse2 = datastoreActions.get("/" + NAMESPACE + "/" + key2 + "/metaData");
    String uid2 = mdResponse2.extractUid();

    // add user to user group
    userActions.post(basicUserId + "/userGroups/" + userGroupId, "").validateStatus(200);

    // set no public access sharing on 1 entry
    QueryParamsBuilder sharingParams =
        new QueryParamsBuilder().add("type", "dataStore").add("id", uid2);
    sharingActions.post("", sharingNoPublicAccess(), sharingParams).validateStatus(200);

    // give access to user for the other entry only and set public access to '--------'
    QueryParamsBuilder params = new QueryParamsBuilder().add("type", "dataStore").add("id", uid1);
    sharingActions.post("", sharingUserGroupAccess(userGroupId), params).validateStatus(200);

    // make call with fields query as user with some access and check can see 1 entry
    loginActions.loginAsUser(BASIC_USER, "Test1234!");

    QueryParamsBuilder fieldsParam = new QueryParamsBuilder().add("fields", "league");
    datastoreActions
        .get("/" + NAMESPACE, fieldsParam)
        .validate()
        .statusCode(200)
        .body("entries.key", allOf(hasItem("arsenal")))
        .body("entries.size()", equalTo(1));
  }

  @Test
  void testDatastoreSharing_NoPublicAccess_UserNoAccess_KeysEndpoint() {
    // add 2 entries as admin
    loginActions.loginAsAdmin();
    String key1 = "arsenal";
    String key2 = "spurs";
    datastoreActions.post("/" + NAMESPACE + "/" + key1, getEntry(key1)).validate().statusCode(201);
    datastoreActions.post("/" + NAMESPACE + "/" + key2, getEntry(key2)).validate().statusCode(201);

    // get ids of entries
    ApiResponse mdResponse1 = datastoreActions.get("/" + NAMESPACE + "/" + key1 + "/metaData");
    String uid1 = mdResponse1.extractUid();
    ApiResponse mdResponse2 = datastoreActions.get("/" + NAMESPACE + "/" + key2 + "/metaData");
    String uid2 = mdResponse2.extractUid();

    // set sharing public access to '--------'
    QueryParamsBuilder sharingParams1 =
        new QueryParamsBuilder().add("type", "dataStore").add("id", uid1);
    sharingActions.post("", sharingNoPublicAccess(), sharingParams1).validateStatus(200);
    QueryParamsBuilder sharingParams2 =
        new QueryParamsBuilder().add("type", "dataStore").add("id", uid2);
    sharingActions.post("", sharingNoPublicAccess(), sharingParams2).validateStatus(200);

    // make call as basic user with no access and check can see no entries
    loginActions.loginAsUser(BASIC_USER, "Test1234!");
    datastoreActions
        .get("/" + NAMESPACE + "/keys")
        .validate()
        .statusCode(200)
        .body("entries.size()", equalTo(0));
  }

  @Test
  void testDatastoreOwnerSharing_NoPublicAccess_UserHasAccess_KeysEndpoint() {
    // add 2 entries as admin
    loginActions.loginAsAdmin();
    String key1 = "arsenal";
    String key2 = "spurs";
    datastoreActions.post("/" + NAMESPACE + "/" + key1, getEntry(key1)).validate().statusCode(201);
    datastoreActions.post("/" + NAMESPACE + "/" + key2, getEntry(key2)).validate().statusCode(201);

    // make call as owner and check can see entries
    loginActions.loginAsUser(BASIC_USER, "Test1234!");
    datastoreActions
        .get("/" + NAMESPACE + "/keys")
        .validate()
        .statusCode(200)
        .body("$", allOf(hasItem("arsenal"), hasItem("spurs")));
  }

  @Test
  void testDatastoreUserSharing_DefaultPublicAccess_KeysEndpoint() {
    // add 2 entries as admin
    loginActions.loginAsAdmin();
    String key1 = "arsenal";
    String key2 = "spurs";
    datastoreActions.post("/" + NAMESPACE + "/" + key1, getEntry(key1)).validate().statusCode(201);
    datastoreActions.post("/" + NAMESPACE + "/" + key2, getEntry(key2)).validate().statusCode(201);

    // make call as basic user and check can see entries
    datastoreActions
        .get("/" + NAMESPACE + "/keys")
        .validate()
        .statusCode(200)
        .body("$", allOf(hasItem("arsenal"), hasItem("spurs")));
  }

  protected static JsonObject getEntry(String team) {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("name", team);
    jsonObject.addProperty("league", "prem");
    return jsonObject;
  }

  protected static String sharingUserAccess(String userId) {
    return """
    {
        "object": {
            "publicAccess": "--------",
            "externalAccess": false,
            "user": {},
            "userAccesses": [
                {
                    "id": "%s",
                    "access": "rw------"
                }
            ],
            "userGroupAccesses": []
        }
    }
    """
        .formatted(userId)
        .strip();
  }

  protected static String sharingUserGroupAccess(String userGroupId) {
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
                    "access": "rw------"
                }
            ]
        }
    }
    """
        .formatted(userGroupId)
        .strip();
  }

  protected static String sharingNoPublicAccess() {
    return """
    {
        "object": {
            "publicAccess": "--------",
            "externalAccess": false,
            "user": {},
            "userAccesses": [],
            "userGroupAccesses": []
        }
    }
    """
        .strip();
  }
}
