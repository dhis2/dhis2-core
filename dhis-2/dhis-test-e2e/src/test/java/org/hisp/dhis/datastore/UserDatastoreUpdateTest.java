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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.hisp.dhis.datastore.DatastoreKeysTest.getEntry;
import static org.hisp.dhis.helpers.JsonParserUtils.toJsonObject;
import static org.hisp.dhis.jsontree.Json.array;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
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
  private static final String KEY = "key3";
  private Gson gson;

  @BeforeAll
  public void beforeAll() {
    userDatastoreActions = new RestApiActions("userDataStore");
    loginActions = new LoginActions();
    loginActions.loginAsSuperUser();
    gson = new Gson();
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

  //  @AfterAll
  //  public void deleteUser() {
  //    loginActions.loginAsSuperUser();
  //  }

  /**
   * @Test * void testUpdateEntry_RootWithNullValue() { * addEntry("ns1", "key1", "42"); *
   * updateEntry("/userDataStore/ns1/key1"); * assertStatus(NOT_FOUND,
   * GET("/userDataStore/ns1/key1")); * }
   */
  //  @Test
  //  @DisplayName("Can update existing key value to null which leads to delete")
  //  void testUpdateEntry_RootWithNullValue() {
  //    // add entry
  //    userDatastoreActions
  //        .update("/" + NAMESPACE + "/" + KEY, "{\"a\":43}")
  //        .validate()
  //        .statusCode(201);
  //
  //    // update entry, removing body
  //    userDatastoreActions.update("/" + NAMESPACE + "/" + KEY, "").validate().statusCode(200);
  //
  //    // confirm entry not found
  //    //    userDatastoreActions.get("/" + NAMESPACE + "/" + "key1").validateStatus(404);
  //  }

  /**
   * @Test void * testUpdateEntry_RootWithNonNullValue() { addEntry("ns3", "key1", "{'a':42}"); *
   * updateEntry("/userDataStore/ns3/key1", Body("7")); assertEquals("7", *
   * GET("/userDataStore/ns3/key1").content().node().getDeclaration()); }
   */
  @Test
  @DisplayName("Can update root with new value")
  void testUpdateEntry_RootWithNonNullValue() {
    // add entry
    userDatastoreActions.update("/" + NAMESPACE + "/" + KEY, json("11")).validate().statusCode(201);

    // update entry, removing body
    userDatastoreActions.update("/" + NAMESPACE + "/" + KEY, json("99")).validate().statusCode(200);

    // confirm entry changed
    userDatastoreActions
        .get("/" + NAMESPACE + "/" + KEY)
        .validateStatus(200)
        .validate()
        .body("a", equalTo("99"));
  }

  /**
   * * @Test void testUpdateEntry_PathWithNullValue() { addEntry("ns2", "key1", "{'a':42}"); *
   * updateEntry("/userDataStore/ns2/key1?path=a"); assertEquals("{\"a\": null}", *
   * GET("/userDataStore/ns2/key1").content().node().getDeclaration()); }
   */
  //  @Test
  //  @DisplayName("Can update path with null value")
  //  void testUpdateEntry_PathWithNullValue() {
  //    // add entry
  //    userDatastoreActions.update("/" + NAMESPACE + "/" + KEY,
  // json("11")).validate().statusCode(201);
  //
  //    // update entry, removing body
  //    userDatastoreActions.update("/" + NAMESPACE + "/" + KEY,
  // json("99")).validate().statusCode(200);
  //
  //    // confirm entry changed
  //    userDatastoreActions
  //        .get("/" + NAMESPACE + "/" + KEY)
  //        .validateStatus(200)
  //        .validate()
  //        .body("a", equalTo("99"));
  //  }

  /**
   * @Test void * testUpdateEntry_PathWithNonNullValue() { addEntry("ns4", "key1", "{'a':42}"); *
   * updateEntry("/userDataStore/ns4/key1?path=a", Body("7")); assertEquals("{\"a\": 7}", *
   * GET("/userDataStore/ns4/key1").content().node().getDeclaration()); }
   */
  @Test
  @DisplayName("Can update path with new value")
  void testUpdateEntry_PathWithNonNullValue() {
    // add entry
    userDatastoreActions.update("/" + NAMESPACE + "/" + KEY, json("11")).validate().statusCode(201);

    // update entry, removing body
    userDatastoreActions
        .update("/" + NAMESPACE + "/" + KEY + "?path=a", "99")
        .validate()
        .statusCode(200);

    // confirm entry changed
    userDatastoreActions
        .get("/" + NAMESPACE + "/" + KEY)
        .validateStatus(200)
        .validate()
        .body("a", equalTo("99"));
  }

  /**
   * @Test void * testUpdateEntry_RollRootValueIsNull() { addEntry("ns5", "key1", "null"); *
   * updateEntry("/userDataStore/ns5/key1?roll=3", Body("7")); assertEquals("[7]", *
   * GET("/userDataStore/ns5/key1").content().node().getDeclaration()); }
   */
  //  @Test
  //  @DisplayName("Can update roll with null value")
  //  void testUpdateEntry_RollRootValueIsNull() {
  //    // add entry
  //    userDatastoreActions.update("/" + NAMESPACE + "/" + KEY,
  // json("11")).validate().statusCode(201);
  //
  //    // update entry, removing body
  //    userDatastoreActions
  //        .update("/" + NAMESPACE + "/" + KEY + "?path=a", "99")
  //        .validate()
  //        .statusCode(200);
  //
  //    // confirm entry changed
  //    userDatastoreActions
  //        .get("/" + NAMESPACE + "/" + KEY)
  //        .validateStatus(200)
  //        .validate()
  //        .body("a", equalTo("99"));
  //  }

  /**
   * @Test void testUpdateEntry_RollRootValueIsArray() { addEntry("ns6", "key1", "[]"); *
   * updateEntry("/userDataStore/ns6/key1?roll=3", Body("7")); assertEquals("[7]", *
   * GET("/userDataStore/ns6/key1").content().node().getDeclaration()); * *
   *
   * <p>updateEntry("/userDataStore/ns6/key1?roll=3", Body("8")); doInTransaction( () -> *
   * assertEquals( "[7, 8]", GET("/userDataStore/ns6/key1").content().node().getDeclaration())); * *
   *
   * <p>updateEntry("/userDataStore/ns6/key1?roll=3", Body("9")); doInTransaction( () -> *
   * assertEquals( "[7, 8, 9]", GET("/userDataStore/ns6/key1").content().node().getDeclaration()));
   * * *
   *
   * <p>updateEntry("/userDataStore/ns6/key1?roll=3", Body("10")); doInTransaction( () -> *
   * assertEquals( "[8, 9, 10]", GET("/userDataStore/ns6/key1").content().node().getDeclaration()));
   * * }
   */
  @Test
  @DisplayName("Can update roll with new array value")
  void testUpdateEntry_RollRootValueIsArray() {
    // add entry
    userDatastoreActions
        .update("/" + NAMESPACE + "/" + KEY, new JsonArray())
        .validate()
        .statusCode(201);

    // update entry, removing body
    userDatastoreActions
        .update("/" + NAMESPACE + "/" + KEY + "?roll=3", 7)
        .validate()
        .statusCode(200);

    // confirm entry changed
    userDatastoreActions
        .get("/" + NAMESPACE + "/" + KEY)
        .validateStatus(200)
        .validate()
        .body("$", hasItems(7));

    // update entry, removing body
    userDatastoreActions
        .update("/" + NAMESPACE + "/" + KEY + "?roll=3", 8)
        .validate()
        .statusCode(200);

    // confirm entry changed
    userDatastoreActions
        .get("/" + NAMESPACE + "/" + KEY)
        .validateStatus(200)
        .validate()
        .body("$", hasItems(7, 8));

    // update entry, removing body
    userDatastoreActions
        .update("/" + NAMESPACE + "/" + KEY + "?roll=3", 9)
        .validate()
        .statusCode(200);

    // confirm entry changed
    userDatastoreActions
        .get("/" + NAMESPACE + "/" + KEY)
        .validateStatus(200)
        .validate()
        .body("$", hasItems(7, 8, 9));
    //
    // update entry, removing body
    userDatastoreActions
        .update("/" + NAMESPACE + "/" + KEY + "?roll=3", 10)
        .validate()
        .statusCode(200);

    // confirm entry changed
    userDatastoreActions
        .get("/" + NAMESPACE + "/" + KEY)
        .validateStatus(200)
        .validate()
        .body("$", hasItems(8, 9, 10))
        .body("$", not(hasItems(7)));
  }

  /**
   * @Test void testUpdateEntry_RollRootValueIsOther() { addEntry("ns7", "key1", "{}"); *
   * updateEntry("/userDataStore/ns7/key1?roll=3", Body("7")); doInTransaction( () -> *
   * assertEquals("7", GET("/userDataStore/ns7/key1").content().node().getDeclaration())); *
   * updateEntry("/userDataStore/ns7/key1?roll=3", Body("\"hello\"")); doInTransaction(() -> *
   * assertEquals("hello", GET("/dataStore/ns7/key1").content().string())); *
   * updateEntry("/userDataStore/ns7/key1?roll=3", Body("true")); doInTransaction(() -> *
   * assertTrue(GET("/userDataStore/ns7/key1").content().booleanValue())); }
   */
  @Test
  @DisplayName("Can update roll with new value")
  void testUpdateEntry_RollRootValueIsOther() {
    // add entry
    userDatastoreActions
        .update("/" + NAMESPACE + "/" + KEY, new JsonObject())
        .validate()
        .statusCode(201);

    // update entry, removing body
    userDatastoreActions
        .update("/" + NAMESPACE + "/" + KEY + "?roll=3", 7)
        .validate()
        .statusCode(200);

    // confirm entry changed
    userDatastoreActions
        .get("/" + NAMESPACE + "/" + KEY)
        .validateStatus(200)
        .validate()
        .body("$", equalTo(7));

    // update entry, removing body
    userDatastoreActions
        .update("/" + NAMESPACE + "/" + KEY + "?roll=3", "hello")
        .validate()
        .statusCode(200);

    // confirm entry changed
    assertEquals(
        "hello",
        userDatastoreActions.get("/" + NAMESPACE + "/" + KEY).validateStatus(200).as(String.class));

    // update entry, removing body
    userDatastoreActions
        .update("/" + NAMESPACE + "/" + KEY + "?roll=3", true)
        .validate()
        .statusCode(200);

    // confirm entry changed
    assertEquals(
        "true",
        userDatastoreActions.get("/" + NAMESPACE + "/" + KEY).validateStatus(200).getAsString());
  }

  /**
   * @Test void testUpdateEntry_RollPathValueIsNull() { addEntry("ns8", "key1", "{'a':null}"); *
   * updateEntry("/userDataStore/ns8/key1?roll=3&path=a", Body("7")); assertEquals("{\"a\": [7]}", *
   * GET("/userDataStore/ns8/key1").content().node().getDeclaration()); }
   */
  @Test
  @DisplayName("Can update roll path with new value")
  void testUpdateEntry_RollPathValueIsNull() {
    // add entry
    userDatastoreActions
        .update("/" + NAMESPACE + "/" + KEY, jsonNull(null))
        .validate()
        .statusCode(201);

    // update entry, removing body
    userDatastoreActions
        .update("/" + NAMESPACE + "/" + KEY + "?roll=3&path=a", 7)
        .validate()
        .statusCode(200);

    // confirm entry changed
    userDatastoreActions
        .get("/" + NAMESPACE + "/" + KEY)
        .validateStatus(200)
        .validate()
        .body("a", hasItems(7));
  }

  /**
   * @Test void * testUpdateEntry_RollPathValueIsUndefined() { addEntry("ns9", "key1",
   * "{'a':null}"); * updateEntry("/userDataStore/ns9/key1?roll=3&path=b", Body("7")); assertEquals(
   * "{\"a\": null, * \"b\": [7]}",
   * GET("/userDataStore/ns9/key1").content().node().getDeclaration()); }
   */
  @Test
  @DisplayName("update roll path undefined")
  void testUpdateEntry_RollPathValueIsUndefined() {
    // add entry
    userDatastoreActions
        .update("/" + NAMESPACE + "/" + KEY, jsonNull(null))
        .validate()
        .statusCode(201);

    // update entry, removing body
    userDatastoreActions
        .update("/" + NAMESPACE + "/" + KEY + "?roll=3&path=b", 7)
        .validate()
        .statusCode(200);

    // confirm entry changed
    userDatastoreActions
        .get("/" + NAMESPACE + "/" + KEY)
        .validateStatus(200)
        .validate()
        .body("a", equalTo(null))
        .body("b", hasItems(7));
  }

  /**
   * @Test void * testUpdateEntry_RollPathValueIsArray() { addEntry("ns10", "key1",
   * "{'a':{'b':[]}}"); * updateEntry("/userDataStore/ns10/key1?roll=3&path=a.b", Body("7"));
   * assertEquals( "[7]", *
   * GET("/userDataStore/ns10/key1").content().get("a.b").node().getDeclaration()); * *
   *
   * <p>updateEntry("/userDataStore/ns10/key1?roll=3&path=a.b", Body("8")); doInTransaction( () -> *
   * assertEquals( "[7, 8]", *
   * GET("/userDataStore/ns10/key1").content().get("a.b").node().getDeclaration())); * *
   *
   * <p>updateEntry("/userDataStore/ns10/key1?roll=3&path=a.b", Body("9")); doInTransaction( () -> *
   * assertEquals( "[7, 8, 9]", *
   * GET("/userDataStore/ns10/key1").content().get("a.b").node().getDeclaration())); * *
   *
   * <p>updateEntry("/userDataStore/ns10/key1?roll=3&path=a.b", Body("10")); doInTransaction( () ->
   * * assertEquals( "[8, 9, 10]", *
   * GET("/userDataStore/ns10/key1").content().get("a.b").node().getDeclaration())); }
   */

  /**
   * @Test void testUpdateEntry_RollPathValueIsOther() { addEntry("ns11", "key1", "{'a':[{}]}");
   * updateEntry("/userDataStore/ns11/key1?roll=3&path=a.[0]", Body("7")); doInTransaction( () ->
   * assertEquals( "{\"a\": [7]}",
   * GET("/userDataStore/ns11/key1").content().node().getDeclaration()));
   *
   * <p>updateEntry("/userDataStore/ns11/key1?roll=3&path=a.[0]", Body("\"hello\""));
   * doInTransaction( () -> assertEquals( "{\"a\": [\"hello\"]}",
   * GET("/userDataStore/ns11/key1").content().node().getDeclaration()));
   *
   * <p>updateEntry("/userDataStore/ns11/key1?roll=3&path=a.[0]", Body("true")); doInTransaction( ()
   * -> assertEquals( "{\"a\": [true]}",
   * GET("/userDataStore/ns11/key1").content().node().getDeclaration())); }
   */
  private JsonObject json(String value) {
    String json = """
      {"a": "%s"}
    """.formatted(value);
    return toJsonObject(json);
  }

  private JsonObject jsonNull(String value) {
    String json = """
      {"a": %s}
    """.formatted(value);
    return toJsonObject(json);
  }

  private JsonArray arrayWithValue(int num) {
    JsonArray array = new JsonArray();
    array.add(num);
    return array;
  }
}
