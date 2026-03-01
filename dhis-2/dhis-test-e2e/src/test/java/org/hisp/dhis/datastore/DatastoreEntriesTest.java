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

import static org.hamcrest.Matchers.equalTo;
import static org.hisp.dhis.datastore.DatastoreKeysTest.getEntry;
import static org.hisp.dhis.datastore.DatastoreKeysTest.sharingNoPublicAccess;
import static org.hisp.dhis.datastore.DatastoreKeysTest.sharingUserAccess;
import static org.hisp.dhis.datastore.DatastoreKeysTest.sharingUserGroupAccess;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonObject;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.test.e2e.actions.LoginActions;
import org.hisp.dhis.test.e2e.actions.RestApiActions;
import org.hisp.dhis.test.e2e.actions.UserActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.junit.jupiter.api.AfterAll;
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
    loginActions.loginAsSuperUser();
    datastoreActions
        .post("/" + NAMESPACE + "/testEntry", getEntry("testEntry"))
        .validate()
        .statusCode(201);
    datastoreActions.delete(NAMESPACE).validateStatus(200);
  }

  @AfterAll
  public void deleteUser() {
    loginActions.loginAsSuperUser();
    userActions.delete(basicUserId).validateStatus(200);
  }

  @Test
  @DisplayName("User can read a datastore entry with default public sharing")
  void testDatastoreSharing_DefaultPublicAccess_BasicUser() {
    // add entry as admin
    loginActions.loginAsAdmin();
    String key1 = "arsenal";
    datastoreActions.post("/" + NAMESPACE + "/" + key1, getEntry(key1)).validate().statusCode(201);

    // make call as basic user and check can see entry
    loginActions.loginAsUser(BASIC_USER, "Test1234!");
    ApiResponse getResponse =
        datastoreActions.get("/" + NAMESPACE + "/" + key1).validateStatus(200);
    assertEquals("{\"name\": \"arsenal\", \"league\": \"prem\"}", getResponse.getAsString());
  }

  @Test
  @DisplayName("User can update a datastore entry with default public sharing")
  void updateEntry_DefaultPublicAccess_BasicUser() {
    // add entry as admin
    loginActions.loginAsAdmin();
    String key1 = "arsenal";
    datastoreActions.post("/" + NAMESPACE + "/" + key1, getEntry(key1)).validate().statusCode(201);

    // make update as basic user and check can see entry
    loginActions.loginAsUser(BASIC_USER, "Test1234!");

    datastoreActions
        .update("/" + NAMESPACE + "/" + key1, getEntry("arsenal update"))
        .validateStatus(200)
        .validate()
        .statusCode(200)
        .body("httpStatus", equalTo("OK"))
        .body("status", equalTo("OK"))
        .body("message", equalTo("Key updated: 'arsenal'"));

    datastoreActions
        .get("/" + NAMESPACE + "/" + key1)
        .validateStatus(200)
        .validate()
        .body("name", equalTo("arsenal update"));
  }

  @Test
  @DisplayName("Superuser can read a datastore entry when public sharing set to none")
  void testDatastoreSharing_NoPublicAccess_SuperUser() {
    // add entry as admin
    loginActions.loginAsAdmin();
    String key1 = "arsenal";
    datastoreActions.post("/" + NAMESPACE + "/" + key1, getEntry(key1)).validate().statusCode(201);

    // get id of entry
    ApiResponse mdResponse1 = datastoreActions.get("/" + NAMESPACE + "/" + key1 + "/metaData");
    String uid1 = mdResponse1.extractUid();

    // set sharing public access to '--------'
    QueryParamsBuilder sharingParams =
        new QueryParamsBuilder().add("type", "dataStore").add("id", uid1);
    sharingActions.post("", sharingNoPublicAccess(), sharingParams).validateStatus(200);

    // make call as superuser and check can see entry
    loginActions.loginAsSuperUser();
    ApiResponse getResponse =
        datastoreActions.get("/" + NAMESPACE + "/" + key1).validateStatus(200);
    assertEquals("{\"name\": \"arsenal\", \"league\": \"prem\"}", getResponse.getAsString());
  }

  @Test
  @DisplayName("Superuser can update a datastore entry with no public sharing")
  void updateEntry_NoPublicAccess_SuperUser() {
    // add entry as admin
    loginActions.loginAsAdmin();
    String key1 = "arsenal";
    datastoreActions.post("/" + NAMESPACE + "/" + key1, getEntry(key1)).validate().statusCode(201);

    // get id of entry
    ApiResponse mdResponse1 = datastoreActions.get("/" + NAMESPACE + "/" + key1 + "/metaData");
    String uid1 = mdResponse1.extractUid();

    // set sharing public access to '--------'
    QueryParamsBuilder sharingParams =
        new QueryParamsBuilder().add("type", "dataStore").add("id", uid1);
    sharingActions.post("", sharingNoPublicAccess(), sharingParams).validateStatus(200);

    // make update as superuser
    loginActions.loginAsSuperUser();

    datastoreActions
        .update("/" + NAMESPACE + "/" + key1, getEntry("arsenal update2"))
        .validateStatus(200)
        .validate()
        .statusCode(200)
        .body("httpStatus", equalTo("OK"))
        .body("status", equalTo("OK"))
        .body("message", equalTo("Key updated: 'arsenal'"));

    // check entry has been updated
    datastoreActions
        .get("/" + NAMESPACE + "/" + key1)
        .validateStatus(200)
        .validate()
        .body("name", equalTo("arsenal update2"));
  }

  @Test
  @DisplayName("Basic user cannot update a datastore entry with no public sharing")
  void updateEntry_NoPublicAccess_BasicUser() {
    // add entry as admin
    loginActions.loginAsAdmin();
    String key1 = "arsenal";
    datastoreActions.post("/" + NAMESPACE + "/" + key1, getEntry(key1)).validate().statusCode(201);

    // get id of entry
    ApiResponse mdResponse1 = datastoreActions.get("/" + NAMESPACE + "/" + key1 + "/metaData");
    String uid1 = mdResponse1.extractUid();

    // set sharing public access to '--------'
    QueryParamsBuilder sharingParams =
        new QueryParamsBuilder().add("type", "dataStore").add("id", uid1);
    sharingActions.post("", sharingNoPublicAccess(), sharingParams).validateStatus(200);

    // make update as basic user
    loginActions.loginAsUser(BASIC_USER, "Test1234!");

    datastoreActions
        .update("/" + NAMESPACE + "/" + key1, getEntry("arsenal update3"))
        .validateStatus(403)
        .validate()
        .body("httpStatus", equalTo("Forbidden"))
        .body("status", equalTo("ERROR"))
        .body("message", equalTo("Access denied for key 'arsenal' in namespace 'football'"));
  }

  @Test
  @DisplayName("User can't read a datastore entry when public sharing set to none")
  void testDatastoreUserSharing_NoPublicAccess_UserNoAccess() {
    // add entry as admin
    loginActions.loginAsAdmin();
    String key1 = "arsenal";
    datastoreActions.post("/" + NAMESPACE + "/" + key1, getEntry(key1)).validate().statusCode(201);

    // get id of entry
    ApiResponse mdResponse1 = datastoreActions.get("/" + NAMESPACE + "/" + key1 + "/metaData");
    String uid1 = mdResponse1.extractUid();

    // set sharing public access to '--------'
    QueryParamsBuilder sharingParams1 =
        new QueryParamsBuilder().add("type", "dataStore").add("id", uid1);
    sharingActions.post("", sharingNoPublicAccess(), sharingParams1).validateStatus(200);

    // make call as user with no access and check can't see entry
    loginActions.loginAsUser(BASIC_USER, "Test1234!");
    ApiResponse getResponse =
        datastoreActions.get("/" + NAMESPACE + "/" + key1).validateStatus(403);

    getResponse
        .validate()
        .body("httpStatus", equalTo("Forbidden"))
        .body("status", equalTo("ERROR"))
        .body("message", equalTo("Access denied for key 'arsenal' in namespace 'football'"))
        .body("errorCode", equalTo("E1006"));
  }

  @Test
  @DisplayName(
      "User can read a datastore entry when public sharing set to none and user has user sharing access")
  void testDatastoreUserSharing_NoPublicAccess_UserHasAccess() {
    // add entry as admin
    loginActions.loginAsAdmin();
    String key1 = "arsenal";
    datastoreActions.post("/" + NAMESPACE + "/" + key1, getEntry(key1)).validate().statusCode(201);

    // get ids of entry
    ApiResponse mdResponse1 = datastoreActions.get("/" + NAMESPACE + "/" + key1 + "/metaData");
    String uid1 = mdResponse1.extractUid();

    // share entry with user and set public access to '--------'
    QueryParamsBuilder params = new QueryParamsBuilder().add("type", "dataStore").add("id", uid1);
    sharingActions.post("", sharingUserAccess(basicUserId), params).validateStatus(200);

    // make call as user with access and check can see entry
    loginActions.loginAsUser(BASIC_USER, "Test1234!");
    ApiResponse getResponse = datastoreActions.get("/" + NAMESPACE + "/" + key1);
    assertEquals("{\"name\": \"arsenal\", \"league\": \"prem\"}", getResponse.getAsString());
  }

  @Test
  @DisplayName(
      "User can update a datastore entry when public sharing set to none and user has user sharing access")
  void updateEntry_NoPublicAccess_BasicUserHasUserSharing() {
    // add entry as admin
    loginActions.loginAsAdmin();
    String key1 = "arsenal";
    datastoreActions.post("/" + NAMESPACE + "/" + key1, getEntry(key1)).validate().statusCode(201);

    // get ids of entry
    ApiResponse mdResponse1 = datastoreActions.get("/" + NAMESPACE + "/" + key1 + "/metaData");
    String uid1 = mdResponse1.extractUid();

    // share entry with user and set public access to '--------'
    QueryParamsBuilder params = new QueryParamsBuilder().add("type", "dataStore").add("id", uid1);
    sharingActions.post("", sharingUserAccess(basicUserId), params).validateStatus(200);

    // make update as basic user
    loginActions.loginAsUser(BASIC_USER, "Test1234!");

    datastoreActions
        .update("/" + NAMESPACE + "/" + key1, getEntry("arsenal update4"))
        .validateStatus(200)
        .validate()
        .statusCode(200)
        .body("httpStatus", equalTo("OK"))
        .body("status", equalTo("OK"))
        .body("message", equalTo("Key updated: 'arsenal'"));

    // check entry has been updated
    datastoreActions
        .get("/" + NAMESPACE + "/" + key1)
        .validateStatus(200)
        .validate()
        .body("name", equalTo("arsenal update4"));
  }

  @Test
  @DisplayName(
      "User can read a datastore entry when public sharing set to none and user has user group sharing access")
  void testDatastoreUserGroupSharing_NoPublicAccess_UserHasAccess() {
    // add entry as admin
    loginActions.loginAsAdmin();
    String key1 = "arsenal";
    datastoreActions.post("/" + NAMESPACE + "/" + key1, getEntry(key1)).validate().statusCode(201);

    // get ids of entry
    ApiResponse mdResponse1 = datastoreActions.get("/" + NAMESPACE + "/" + key1 + "/metaData");
    String uid1 = mdResponse1.extractUid();

    // add user to user group
    userActions.post(basicUserId + "/userGroups/" + userGroupId, "").validateStatus(200);

    // share entries with user group and set public access to '--------'
    QueryParamsBuilder params = new QueryParamsBuilder().add("type", "dataStore").add("id", uid1);
    sharingActions.post("", sharingUserGroupAccess(userGroupId), params).validateStatus(200);

    // make call as user with access and check can see entry
    loginActions.loginAsUser(BASIC_USER, "Test1234!");
    ApiResponse getResponse =
        datastoreActions.get("/" + NAMESPACE + "/" + key1).validateStatus(200);
    assertEquals("{\"name\": \"arsenal\", \"league\": \"prem\"}", getResponse.getAsString());
  }

  @Test
  @DisplayName(
      "User can update a datastore entry when public sharing set to none and user has group sharing access")
  void updateEntry_NoPublicAccess_BasicUserHasGroupSharing() {
    // add entry as admin
    loginActions.loginAsAdmin();
    String key1 = "arsenal";
    datastoreActions.post("/" + NAMESPACE + "/" + key1, getEntry(key1)).validate().statusCode(201);

    // get ids of entry
    ApiResponse mdResponse1 = datastoreActions.get("/" + NAMESPACE + "/" + key1 + "/metaData");
    String uid1 = mdResponse1.extractUid();

    // add user to user group
    userActions.post(basicUserId + "/userGroups/" + userGroupId, "").validateStatus(200);

    // share entries with user group and set public access to '--------'
    QueryParamsBuilder params = new QueryParamsBuilder().add("type", "dataStore").add("id", uid1);
    sharingActions.post("", sharingUserGroupAccess(userGroupId), params).validateStatus(200);

    // make update as basic user
    loginActions.loginAsUser(BASIC_USER, "Test1234!");

    datastoreActions
        .update("/" + NAMESPACE + "/" + key1, getEntry("arsenal update5"))
        .validateStatus(200)
        .validate()
        .statusCode(200)
        .body("httpStatus", equalTo("OK"))
        .body("status", equalTo("OK"))
        .body("message", equalTo("Key updated: 'arsenal'"));

    // check entry has been updated
    datastoreActions
        .get("/" + NAMESPACE + "/" + key1)
        .validateStatus(200)
        .validate()
        .body("name", equalTo("arsenal update5"));
  }

  @Test
  @DisplayName("User can read datastore entry metadata with default public sharing")
  void testDatastoreMetadataSharing_DefaultPublicAccess_BasicUser() {
    // add entry as admin
    loginActions.loginAsAdmin();
    String key1 = "arsenal";
    datastoreActions.post("/" + NAMESPACE + "/" + key1, getEntry(key1)).validate().statusCode(201);

    // make call as user and check can see entry metadata
    loginActions.loginAsUser(BASIC_USER, "Test1234!");
    ApiResponse getResponse =
        datastoreActions.get("/" + NAMESPACE + "/" + key1 + "/metaData").validateStatus(200);
    JsonObject createdBy = getResponse.getBody().getAsJsonObject("createdBy");
    assertEquals(
        "{\"id\":\"PQD6wXJ2r5k\",\"code\":null,\"name\":\"TA Admin\",\"displayName\":\"TA Admin\"}",
        createdBy.toString());
  }

  @Test
  @DisplayName("Superuser can read datastore entry metadata when public sharing set to none")
  void testDatastoreMetadataSharing_NoPublicAccess_SuperUser() {
    // add entry as admin
    loginActions.loginAsAdmin();
    String key1 = "arsenal";
    datastoreActions.post("/" + NAMESPACE + "/" + key1, getEntry(key1)).validate().statusCode(201);

    // get ids of entries
    ApiResponse mdResponse1 = datastoreActions.get("/" + NAMESPACE + "/" + key1 + "/metaData");
    String uid1 = mdResponse1.extractUid();

    // set sharing public access to '--------'
    QueryParamsBuilder sharingParams =
        new QueryParamsBuilder().add("type", "dataStore").add("id", uid1);
    sharingActions.post("", sharingNoPublicAccess(), sharingParams).validateStatus(200);

    // make call as superuser and check can see entry metadata
    loginActions.loginAsSuperUser();
    ApiResponse getResponse =
        datastoreActions.get("/" + NAMESPACE + "/" + key1 + "/metaData").validateStatus(200);
    JsonObject createdBy = getResponse.getBody().getAsJsonObject("createdBy");
    assertEquals(
        "{\"id\":\"PQD6wXJ2r5k\",\"code\":null,\"name\":\"TA Admin\",\"displayName\":\"TA Admin\"}",
        createdBy.toString());
  }

  @Test
  @DisplayName("User can't read datastore entry metadata when public sharing set to none")
  void testDatastoreMetadataUserSharing_NoPublicAccess_UserNoAccess() {
    // add entry as admin
    loginActions.loginAsAdmin();
    String key1 = "arsenal";
    datastoreActions.post("/" + NAMESPACE + "/" + key1, getEntry(key1)).validate().statusCode(201);

    // get id of entry
    ApiResponse mdResponse1 = datastoreActions.get("/" + NAMESPACE + "/" + key1 + "/metaData");
    String uid1 = mdResponse1.extractUid();

    // set sharing public access to '--------'
    QueryParamsBuilder sharingParams1 =
        new QueryParamsBuilder().add("type", "dataStore").add("id", uid1);
    sharingActions.post("", sharingNoPublicAccess(), sharingParams1).validateStatus(200);

    // make call as basic user with no access and check can't see entry metadata
    loginActions.loginAsUser(BASIC_USER, "Test1234!");
    ApiResponse getResponse =
        datastoreActions.get("/" + NAMESPACE + "/" + key1 + "/metaData").validateStatus(403);

    getResponse
        .validate()
        .body("httpStatus", equalTo("Forbidden"))
        .body("status", equalTo("ERROR"))
        .body("message", equalTo("Access denied for key 'arsenal' in namespace 'football'"))
        .body("errorCode", equalTo("E1006"));
  }

  @Test
  @DisplayName(
      "User can read datastore entry metadata when public sharing set to none and user has user sharing access")
  void testDatastoreMetadataUserSharing_NoPublicAccess_UserHasAccess() {
    // add entry as admin
    loginActions.loginAsAdmin();
    String key1 = "arsenal";
    datastoreActions.post("/" + NAMESPACE + "/" + key1, getEntry(key1)).validate().statusCode(201);

    // get ids of entry
    ApiResponse mdResponse1 = datastoreActions.get("/" + NAMESPACE + "/" + key1 + "/metaData");
    String uid1 = mdResponse1.extractUid();

    // share entry with user and set public access to '--------'
    QueryParamsBuilder params = new QueryParamsBuilder().add("type", "dataStore").add("id", uid1);
    sharingActions.post("", sharingUserAccess(basicUserId), params).validateStatus(200);

    // make call as user with access and check can see entry metadata
    loginActions.loginAsUser(BASIC_USER, "Test1234!");
    ApiResponse getResponse = datastoreActions.get("/" + NAMESPACE + "/" + key1 + "/metaData");
    JsonObject createdBy = getResponse.getBody().getAsJsonObject("createdBy");
    assertEquals(
        "{\"id\":\"PQD6wXJ2r5k\",\"code\":null,\"name\":\"TA Admin\",\"displayName\":\"TA Admin\"}",
        createdBy.toString());
  }

  @Test
  @DisplayName(
      "User can read datastore entry metadata when public sharing set to none and user has user group sharing access")
  void testDatastoreMetadataUserGroupSharing_NoPublicAccess_UserHasAccess() {
    // add entry as admin
    loginActions.loginAsAdmin();
    String key1 = "arsenal";
    datastoreActions.post("/" + NAMESPACE + "/" + key1, getEntry(key1)).validate().statusCode(201);

    // get ids of entry
    ApiResponse mdResponse1 = datastoreActions.get("/" + NAMESPACE + "/" + key1 + "/metaData");
    String uid1 = mdResponse1.extractUid();

    // add user to user group
    userActions.post(basicUserId + "/userGroups/" + userGroupId, "").validateStatus(200);

    // share entries with user group and set public access to '--------'
    QueryParamsBuilder params = new QueryParamsBuilder().add("type", "dataStore").add("id", uid1);
    sharingActions.post("", sharingUserGroupAccess(userGroupId), params).validateStatus(200);

    // make call as user with access and check can see entry metadata
    loginActions.loginAsUser(BASIC_USER, "Test1234!");
    ApiResponse getResponse =
        datastoreActions.get("/" + NAMESPACE + "/" + key1 + "/metaData").validateStatus(200);
    JsonObject createdBy = getResponse.getBody().getAsJsonObject("createdBy");
    assertEquals(
        "{\"id\":\"PQD6wXJ2r5k\",\"code\":null,\"name\":\"TA Admin\",\"displayName\":\"TA Admin\"}",
        createdBy.toString());
  }

  @Test
  @DisplayName("User can delete a datastore entry with default public sharing")
  void testDatastoreSharing_DefaultPublicAccess_BasicUserDelete() {
    // add entry as admin
    loginActions.loginAsAdmin();
    String key1 = "arsenal";
    datastoreActions.post("/" + NAMESPACE + "/" + key1, getEntry(key1)).validate().statusCode(201);

    // make call as basic user and check can delete entry
    loginActions.loginAsUser(BASIC_USER, "Test1234!");
    datastoreActions
        .delete("/" + NAMESPACE + "/" + key1)
        .validateStatus(200)
        .validate()
        .body("httpStatus", equalTo("OK"))
        .body("status", equalTo("OK"))
        .body("message", equalTo("Key 'arsenal' deleted from namespace 'football'"));

    datastoreActions.get("/" + NAMESPACE + "/" + key1).validateStatus(404);
  }

  @Test
  @DisplayName("Superuser can delete a datastore entry when public sharing set to none")
  void testDatastoreSharing_NoPublicAccess_SuperUserDelete() {
    // add entry as admin
    loginActions.loginAsAdmin();
    String key1 = "arsenal";
    datastoreActions.post("/" + NAMESPACE + "/" + key1, getEntry(key1)).validate().statusCode(201);

    // get ids of entries
    ApiResponse mdResponse1 = datastoreActions.get("/" + NAMESPACE + "/" + key1 + "/metaData");
    String uid1 = mdResponse1.extractUid();

    // set sharing public access to '--------'
    QueryParamsBuilder sharingParams =
        new QueryParamsBuilder().add("type", "dataStore").add("id", uid1);
    sharingActions.post("", sharingNoPublicAccess(), sharingParams).validateStatus(200);

    // make call as superuser and check can delete entry
    loginActions.loginAsSuperUser();
    datastoreActions
        .delete("/" + NAMESPACE + "/" + key1)
        .validateStatus(200)
        .validate()
        .statusCode(200)
        .body("httpStatus", equalTo("OK"))
        .body("status", equalTo("OK"))
        .body("message", equalTo("Key 'arsenal' deleted from namespace 'football'"));

    datastoreActions.get("/" + NAMESPACE + "/" + key1).validateStatus(404);
  }

  @Test
  @DisplayName(
      "User can't delete a datastore entry when public access set to none and no user sharing access")
  void testDatastoreUserSharing_NoPublicAccess_UserNoAccessDelete() {
    // add entry as admin
    loginActions.loginAsAdmin();
    String key1 = "arsenal";
    datastoreActions.post("/" + NAMESPACE + "/" + key1, getEntry(key1)).validate().statusCode(201);

    // get id of entry
    ApiResponse mdResponse1 = datastoreActions.get("/" + NAMESPACE + "/" + key1 + "/metaData");
    String uid1 = mdResponse1.extractUid();

    // set sharing public access to '--------'
    QueryParamsBuilder sharingParams1 =
        new QueryParamsBuilder().add("type", "dataStore").add("id", uid1);
    sharingActions.post("", sharingNoPublicAccess(), sharingParams1).validateStatus(200);

    // make call as user with no access and check can't delete entry
    loginActions.loginAsUser(BASIC_USER, "Test1234!");
    datastoreActions
        .delete("/" + NAMESPACE + "/" + key1)
        .validateStatus(403)
        .validate()
        .body("httpStatus", equalTo("Forbidden"))
        .body("status", equalTo("ERROR"))
        .body("message", equalTo("Access denied for key 'arsenal' in namespace 'football'"));
    ;
  }

  @Test
  @DisplayName(
      "User can delete a datastore entry when public access set to none and user has user sharing access")
  void testDatastoreUserSharing_NoPublicAccess_UserHasAccessDelete() {
    // add entry as admin
    loginActions.loginAsAdmin();
    String key1 = "arsenal";
    datastoreActions.post("/" + NAMESPACE + "/" + key1, getEntry(key1)).validate().statusCode(201);

    // get ids of entry
    ApiResponse mdResponse1 = datastoreActions.get("/" + NAMESPACE + "/" + key1 + "/metaData");
    String uid1 = mdResponse1.extractUid();

    // share entry with user and set public access to '--------'
    QueryParamsBuilder params = new QueryParamsBuilder().add("type", "dataStore").add("id", uid1);
    sharingActions.post("", sharingUserAccess(basicUserId), params).validateStatus(200);

    // make call as user with access and check can delete entry
    loginActions.loginAsUser(BASIC_USER, "Test1234!");
    datastoreActions
        .delete("/" + NAMESPACE + "/" + key1)
        .validateStatus(200)
        .validate()
        .statusCode(200)
        .body("httpStatus", equalTo("OK"))
        .body("status", equalTo("OK"))
        .body("message", equalTo("Key 'arsenal' deleted from namespace 'football'"));

    datastoreActions.get("/" + NAMESPACE + "/" + key1).validateStatus(404);
  }

  @Test
  @DisplayName(
      "User can delete a datastore entry when public access set to none and user has user group sharing access")
  void testDatastoreUserGroupSharing_NoPublicAccess_UserHasAccessDelete() {
    // add entry as admin
    loginActions.loginAsAdmin();
    String key1 = "arsenal";
    datastoreActions.post("/" + NAMESPACE + "/" + key1, getEntry(key1)).validate().statusCode(201);

    // get ids of entry
    ApiResponse mdResponse1 = datastoreActions.get("/" + NAMESPACE + "/" + key1 + "/metaData");
    String uid1 = mdResponse1.extractUid();

    // add user to user group
    userActions.post(basicUserId + "/userGroups/" + userGroupId, "").validateStatus(200);

    // share entries with user group and set public access to '--------'
    QueryParamsBuilder params = new QueryParamsBuilder().add("type", "dataStore").add("id", uid1);
    sharingActions.post("", sharingUserGroupAccess(userGroupId), params).validateStatus(200);

    // make call as user with access and check can delete entry
    loginActions.loginAsUser(BASIC_USER, "Test1234!");
    datastoreActions
        .delete("/" + NAMESPACE + "/" + key1)
        .validateStatus(200)
        .validate()
        .statusCode(200)
        .body("httpStatus", equalTo("OK"))
        .body("status", equalTo("OK"))
        .body("message", equalTo("Key 'arsenal' deleted from namespace 'football'"));

    datastoreActions.get("/" + NAMESPACE + "/" + key1).validateStatus(404);
  }
}
