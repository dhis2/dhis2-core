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
package org.hisp.dhis.datastore;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.hisp.dhis.datastore.DatastoreKeysTest.getEntry;
import static org.hisp.dhis.test.e2e.helpers.JsonParserUtils.toJsonObject;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.test.e2e.actions.LoginActions;
import org.hisp.dhis.test.e2e.actions.RestApiActions;
import org.hisp.dhis.test.e2e.actions.UserActions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author david mackessy
 */
class UserDatastoreUpdateTest extends ApiTest {

  private RestApiActions userDatastoreActions;
  private LoginActions loginActions;
  private static final String NAMESPACE = "ns1";

  @BeforeAll
  public void beforeAll() {
    userDatastoreActions = new RestApiActions("userDataStore");
    loginActions = new LoginActions();
    loginActions.loginAsSuperUser();
  }

  @AfterEach
  public void deleteEntries() {
    loginActions.loginAsSuperUser();
    userDatastoreActions
        .post("/" + NAMESPACE + "/testkey", getEntry("testEntry"))
        .validate()
        .statusCode(201);
    userDatastoreActions.delete(NAMESPACE).validateStatus(200);
  }

  @Test
  @DisplayName("When updating existing entry root value to null, the entry will be deleted")
  void testUpdateEntry_RootWithNullValue() {
    // given
    userDatastoreActions.update("/" + NAMESPACE + "/key8", 42).validate().statusCode(201);

    // when
    userDatastoreActions.updateNoBody("/" + NAMESPACE + "/key8").validate().statusCode(200);

    // then
    userDatastoreActions.get("/" + NAMESPACE + "/" + "key8").validateStatus(404);
  }

  @Test
  @DisplayName("Can update root value with new value")
  void testUpdateEntry_RootWithNonNullValue() {
    // given
    userDatastoreActions
        .update(
            "/" + NAMESPACE + "/key7",
            toJsonObject(
                """
                {"a": 11}
                """))
        .validate()
        .statusCode(201);

    // when
    userDatastoreActions
        .update(
            "/" + NAMESPACE + "/key7",
            toJsonObject(
                """
                {"a": 99}
                """))
        .validate()
        .statusCode(200);

    // then
    userDatastoreActions
        .get("/" + NAMESPACE + "/key7")
        .validateStatus(200)
        .validate()
        .body("a", equalTo(99));
  }

  @Test
  @DisplayName("Can update path with null value")
  void testUpdateEntry_PathWithNullValue() {
    // given
    userDatastoreActions
        .update(
            "/" + NAMESPACE + "/key6",
            toJsonObject(
                """
                {"a": 42}
                """))
        .validate()
        .statusCode(201);

    // when
    userDatastoreActions.updateNoBody("/" + NAMESPACE + "/key6?path=a").validate().statusCode(200);

    // then
    userDatastoreActions
        .get("/" + NAMESPACE + "/key6")
        .validateStatus(200)
        .validate()
        .body("a", equalTo(null));
  }

  @Test
  @DisplayName("Can update path with new value")
  void testUpdateEntry_PathWithNonNullValue() {
    // given
    userDatastoreActions
        .update(
            "/" + NAMESPACE + "/key5",
            toJsonObject(
                """
                {"a": 11}
                """))
        .validate()
        .statusCode(201);

    // when
    userDatastoreActions.update("/" + NAMESPACE + "/key5?path=a", 99).validate().statusCode(200);

    // then
    userDatastoreActions
        .get("/" + NAMESPACE + "/key5")
        .validateStatus(200)
        .validate()
        .body("a", equalTo(99));
  }

  @Test
  @DisplayName("Can update entry that has a null value, with roll")
  void testUpdateEntry_RollRootValueIsNull() {
    // given
    userDatastoreActions.updateNoBody("/" + NAMESPACE + "/key4").validate().statusCode(201);

    // when
    userDatastoreActions.update("/" + NAMESPACE + "/key4?roll=3", 7).validate().statusCode(200);

    // then
    userDatastoreActions
        .get("/" + NAMESPACE + "/key4")
        .validateStatus(200)
        .validate()
        .body("$", equalTo(7));
  }

  @Test
  @DisplayName("Can update root roll with new values and roll value is respected")
  void testUpdateEntry_RollRootValueIsArray() {
    // given
    userDatastoreActions
        .update("/" + NAMESPACE + "/key13", new JsonArray())
        .validate()
        .statusCode(201);

    // when
    userDatastoreActions.update("/" + NAMESPACE + "/key13?roll=3", 7).validate().statusCode(200);

    // then
    userDatastoreActions
        .get("/" + NAMESPACE + "/key13")
        .validateStatus(200)
        .validate()
        .body("$", hasItems(7));

    // when
    userDatastoreActions.update("/" + NAMESPACE + "/key13?roll=3", 8).validate().statusCode(200);

    // then
    userDatastoreActions
        .get("/" + NAMESPACE + "/key13")
        .validateStatus(200)
        .validate()
        .body("$", hasItems(7, 8));

    // when
    userDatastoreActions.update("/" + NAMESPACE + "/key13?roll=3", 9).validate().statusCode(200);

    // then
    userDatastoreActions
        .get("/" + NAMESPACE + "/key13")
        .validateStatus(200)
        .validate()
        .body("$", hasItems(7, 8, 9));

    // when
    userDatastoreActions.update("/" + NAMESPACE + "/key13?roll=3", 10).validate().statusCode(200);

    // then
    userDatastoreActions
        .get("/" + NAMESPACE + "/key13")
        .validateStatus(200)
        .validate()
        .body("$", hasItems(8, 9, 10))
        .body("$", not(hasItems(7)));
  }

  @Test
  @DisplayName("Can update roll with new value")
  void testUpdateEntry_RollRootValueIsOther() {
    // given
    userDatastoreActions
        .update("/" + NAMESPACE + "/key35", new JsonObject())
        .validate()
        .statusCode(201);

    // when
    userDatastoreActions.update("/" + NAMESPACE + "/key35?roll=3", 7).validate().statusCode(200);

    // then
    userDatastoreActions
        .get("/" + NAMESPACE + "/key35")
        .validateStatus(200)
        .validate()
        .body("$", equalTo(7));

    // when
    userDatastoreActions
        .update("/" + NAMESPACE + "/key35?roll=3", "hello")
        .validate()
        .statusCode(200);

    // then
    assertEquals(
        "hello",
        userDatastoreActions.get("/" + NAMESPACE + "/key35").validateStatus(200).as(String.class));

    // when
    userDatastoreActions.update("/" + NAMESPACE + "/key35?roll=3", true).validate().statusCode(200);

    // then
    assertEquals(
        "true",
        userDatastoreActions.get("/" + NAMESPACE + "/key35").validateStatus(200).getAsString());
  }

  @Test
  @DisplayName("Can update roll null path with new value")
  void testUpdateEntry_RollPathValueIsNull() {
    // given
    userDatastoreActions
        .update(
            "/" + NAMESPACE + "/key55",
            toJsonObject(
                """
                {"a": null}
                """))
        .validate()
        .statusCode(201);

    // when
    userDatastoreActions
        .update("/" + NAMESPACE + "/key55?roll=3&path=a", 7)
        .validate()
        .statusCode(200);

    // then
    userDatastoreActions
        .get("/" + NAMESPACE + "/key55")
        .validateStatus(200)
        .validate()
        .body("a", hasItems(7));
  }

  @Test
  @DisplayName("update roll undefined path creates new path")
  void testUpdateEntry_RollPathValueIsUndefined() {
    // given
    userDatastoreActions
        .update(
            "/" + NAMESPACE + "/key24",
            toJsonObject(
                """
                {"a": null}
                """))
        .validate()
        .statusCode(201);

    // when
    userDatastoreActions
        .update("/" + NAMESPACE + "/key24?roll=3&path=b", 7)
        .validate()
        .statusCode(200);

    // then
    userDatastoreActions
        .get("/" + NAMESPACE + "/key24")
        .validateStatus(200)
        .validate()
        .body("a", equalTo(null))
        .body("b", hasItems(7));
  }

  @Test
  @DisplayName("Can update nested roll path with new values and roll value is respected")
  void testUpdateEntry_RollPathValueIsArray() {
    // given
    userDatastoreActions
        .update(
            "/" + NAMESPACE + "/key2",
            toJsonObject(
                """
                  {"a":{"b":[]}}
                  """))
        .validate()
        .statusCode(201);

    // when
    userDatastoreActions
        .update("/" + NAMESPACE + "/key2?roll=3&path=a.b", 7)
        .validate()
        .statusCode(200);

    // then
    userDatastoreActions
        .get("/" + NAMESPACE + "/key2")
        .validateStatus(200)
        .validate()
        .body("a.b", hasItems(7));

    // when
    userDatastoreActions
        .update("/" + NAMESPACE + "/key2?roll=3&path=a.b", 8)
        .validate()
        .statusCode(200);

    // then
    userDatastoreActions
        .get("/" + NAMESPACE + "/key2")
        .validateStatus(200)
        .validate()
        .body("a.b", hasItems(7, 8));

    // when
    userDatastoreActions
        .update("/" + NAMESPACE + "/key2?roll=3&path=a.b", 9)
        .validate()
        .statusCode(200);

    // then
    userDatastoreActions
        .get("/" + NAMESPACE + "/key2")
        .validateStatus(200)
        .validate()
        .body("a.b", hasItems(7, 8, 9));

    // when
    userDatastoreActions
        .update("/" + NAMESPACE + "/key2?roll=3&path=a.b", 10)
        .validate()
        .statusCode(200);

    // then
    userDatastoreActions
        .get("/" + NAMESPACE + "/key2")
        .validateStatus(200)
        .validate()
        .body("a.b", hasItems(8, 9, 10))
        .body("a.b", not(hasItems(7)));
  }

  @Test
  @DisplayName("Can update roll path value as another type")
  void testUpdateEntry_RollPathValueIsOther() {
    // given
    userDatastoreActions
        .update(
            "/" + NAMESPACE + "/key3",
            toJsonObject(
                """
                  {"a":[{}]}
                  """))
        .validate()
        .statusCode(201);

    // when
    userDatastoreActions
        .update("/" + NAMESPACE + "/key3?roll=3&path=a.[0]", 7)
        .validate()
        .statusCode(200);

    // then
    userDatastoreActions
        .get("/" + NAMESPACE + "/key3")
        .validateStatus(200)
        .validate()
        .body("a", hasItems(7));

    // when
    userDatastoreActions
        .update("/" + NAMESPACE + "/key3?roll=3&path=a.[0]", "hello")
        .validate()
        .statusCode(200);

    // then
    userDatastoreActions
        .get("/" + NAMESPACE + "/key3")
        .validateStatus(200)
        .validate()
        .body("a", hasItems("hello"));

    // when
    userDatastoreActions
        .update("/" + NAMESPACE + "/key3?roll=3&path=a.[0]", true)
        .validate()
        .statusCode(200);

    // then
    userDatastoreActions
        .get("/" + NAMESPACE + "/key3")
        .validateStatus(200)
        .validate()
        .body("a", hasItems(true));
  }

  @Test
  @DisplayName(
      "Superuser can update an existing entry passing a username and the user can then see the change")
  void testPutExistingUserKeyJsonValue() {
    // given
    UserActions userActions = new UserActions();
    userActions.addUser("Paul", "Test1234!");
    loginActions.loginAsUser("Paul", "Test1234!");
    userDatastoreActions.update("/" + NAMESPACE + "/key99", "first").validate().statusCode(201);

    // when
    loginActions.loginAsSuperUser();
    userDatastoreActions
        .update("/" + NAMESPACE + "/key99?username=Paul", "last")
        .validate()
        .statusCode(200);

    // then
    loginActions.loginAsUser("Paul", "Test1234!");
    assertEquals("last", userDatastoreActions.get("/" + NAMESPACE + "/key99").as(String.class));
    loginActions.loginAsSuperUser();
  }
}
