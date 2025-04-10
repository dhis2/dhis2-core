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

import static org.hisp.dhis.test.utils.JavaToJson.toJson;
import static org.hisp.dhis.test.webapi.Assertions.assertJson;
import static org.hisp.dhis.test.webapi.Assertions.assertWebMessage;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.hisp.dhis.datastore.DatastoreParams;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.webapi.controller.AbstractUserDatastoreControllerTest;
import org.hisp.dhis.webapi.controller.UserDatastoreController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests specifically the {@code filter} parameter aspect of the {@link
 * UserDatastoreController#getEntries(String, String, String, boolean, DatastoreParams,
 * HttpServletResponse)} method using (mocked) REST requests.
 *
 * <p>Tests will use {@code fields} parameter as it is a required parameter for the API.
 *
 * @author Jan Bernitt
 */
class UserDatastoreFilterControllerTest extends AbstractUserDatastoreControllerTest {
  @BeforeEach
  void setUp() {
    // simple values
    postEntry("pets", "dog", toJson(false));
    postEntry("pets", "bat", toJson(true));
    postEntry("pets", "pidgin", toJson(42));
    postEntry("pets", "crow", toJson("42"));
    postEntry("pets", "horse", toJson("Fury"));
    postEntry("pets", "snake", toJson((Object) null));

    // objects
    postPet("cat", "Miao", 9, List.of("tuna", "mice", "birds"));
    postPet("cow", "Muuhh", 5, List.of("gras"));
    postPet("hamster", "Speedy", 2, List.of("veggies"));
    postPet("pig", "Oink", 6, List.of("carrots", "potatoes"));
  }

  /*
   * OBS! Note that in H2 most filters do not (yet) work, so we can only test
   * filtering using the key property but this shares most of the code with
   * filtering on JSON value which only has special path to extract the value.
   */

  @Test
  void testFilter_Null_Key() {
    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        "Illegal filter `_:null`: key filters cannot be used with unary operators",
        GET("/userDataStore/pets?fields=&headless=true&filter=_:null")
            .content(HttpStatus.CONFLICT));
  }

  @Test
  void testFilter_NotNull_Key() {
    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        "Illegal filter `_:!null`: key filters cannot be used with unary operators",
        GET("/userDataStore/pets?fields=&headless=true&filter=_:!null")
            .content(HttpStatus.CONFLICT));
  }

  @Test
  void testFilter_Empty_Key() {
    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        "Illegal filter `_:empty`: key filters cannot be used with unary operators",
        GET("/userDataStore/pets?fields=&headless=true&filter=_:empty")
            .content(HttpStatus.CONFLICT));
  }

  @Test
  void testFilter_NotEmpty_Key() {
    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        "Illegal filter `_:!empty`: key filters cannot be used with unary operators",
        GET("/userDataStore/pets?fields=&headless=true&filter=_:!empty")
            .content(HttpStatus.CONFLICT));
  }

  @Test
  void testFilter_In_Key() {
    assertJson(
        "[{'key':'cat'},{'key':'dog'}]",
        GET("/userDataStore/pets?fields=&headless=true&filter=_:in:[cat,dog]"));
  }

  @Test
  void testFilter_NotIn_Key() {
    assertJson(
        "[{'key':'bat'},{'key':'cow'},{'key':'crow'},{'key':'hamster'},"
            + "{'key':'horse'},{'key':'pidgin'},{'key':'pig'},{'key':'snake'}]",
        GET("/userDataStore/pets?fields=&headless=true&filter=_:!in:[cat,dog]"));
  }

  @Test
  void testFilter_Eq_Key() {
    assertJson("[{'key':'cat'}]", GET("/userDataStore/pets?fields=&headless=true&filter=_:eq:cat"));
  }

  @Test
  void testFilter_Ieq_Key() {
    assertJson(
        "[{'key':'cat'}]", GET("/userDataStore/pets?fields=&headless=true&filter=_:ieq:CAT"));
  }

  @Test
  void testFilter_NotEq_Key() {
    assertJson(
        "[{'key':'bat'},{'key':'cow'},{'key':'crow'},{'key':'dog'},{'key':'hamster'},"
            + "{'key':'horse'},{'key':'pidgin'},{'key':'pig'},{'key':'snake'}]",
        GET("/userDataStore/pets?fields=&headless=true&filter=_:!eq:cat"));
  }

  @Test
  void testFilter_Lt_Key() {
    assertJson("[{'key':'bat'}]", GET("/userDataStore/pets?fields=&headless=true&filter=_:lt:cat"));
  }

  @Test
  void testFilter_Le_Key() {
    assertJson(
        "[{'key':'bat'},{'key':'cat'}]",
        GET("/userDataStore/pets?fields=&headless=true&filter=_:le:cat"));
  }

  @Test
  void testFilter_Gt_Key() {
    assertJson(
        "[{'key':'cow'},{'key':'crow'},{'key':'dog'},{'key':'hamster'},{'key':'horse'},{'key':'pidgin'},{'key':'pig'},{'key':'snake'}]",
        GET("/userDataStore/pets?fields=&headless=true&filter=_:gt:cat"));
  }

  @Test
  void testFilter_Ge_Key() {
    assertJson(
        "[{'key':'pig'},{'key':'snake'}]",
        GET("/userDataStore/pets?fields=&headless=true&filter=_:ge:pig"));
  }

  @Test
  void testFilter_Like_Key() {
    assertJson(
        "[{'key':'pidgin'},{'key':'pig'}]",
        GET("/userDataStore/pets?fields=&headless=true&filter=_:like:pi"));
  }

  @Test
  void testFilter_NotLike_Key() {
    assertJson(
        "[{'key':'bat'},{'key':'cat'},{'key':'cow'},{'key':'crow'},"
            + "{'key':'dog'},{'key':'hamster'},{'key':'horse'},{'key':'snake'}]",
        GET("/userDataStore/pets?fields=&headless=true&filter=_:!like:pi"));
  }

  @Test
  void testFilter_ILike_Key() {
    assertJson(
        "[{'key':'pidgin'},{'key':'pig'}]",
        GET("/userDataStore/pets?fields=&headless=true&filter=_:ilike:PI"));
  }

  @Test
  void testFilter_NotILike_Key() {
    assertJson(
        "[{'key':'bat'},{'key':'cat'},{'key':'cow'},{'key':'crow'},{'key':'dog'},"
            + "{'key':'hamster'},{'key':'horse'},{'key':'snake'}]",
        GET("/userDataStore/pets?fields=&headless=true&filter=_:!ilike:PI"));
  }

  @Test
  void testFilter_StartsLike_Key() {
    assertJson(
        "[{'key':'pidgin'},{'key':'pig'}]",
        GET("/userDataStore/pets?fields=&headless=true&filter=_:$like:pi"));
  }

  @Test
  void testFilter_NotStartsLike_Key() {
    assertJson(
        "[{'key':'bat'},{'key':'cat'},{'key':'cow'},{'key':'crow'},"
            + "{'key':'dog'},{'key':'hamster'},{'key':'horse'},{'key':'snake'}]",
        GET("/userDataStore/pets?fields=&headless=true&filter=_:!$like:pi"));
  }

  @Test
  void testFilter_EndsLike_Key() {
    assertJson(
        "[{'key':'pig'}]", GET("/userDataStore/pets?fields=&headless=true&filter=_:like$:ig"));
  }

  @Test
  void testFilter_NotEndsLike_Key() {
    assertJson(
        "[{'key':'bat'},{'key':'cat'},{'key':'cow'},{'key':'crow'},{'key':'dog'},"
            + "{'key':'hamster'},{'key':'horse'},{'key':'pidgin'},{'key':'snake'}]",
        GET("/userDataStore/pets?fields=&headless=true&filter=_:!like$:ig"));
  }

  @Test
  void testFilter_StartsWith_Key() {
    assertJson(
        "[{'key':'pidgin'},{'key':'pig'}]",
        GET("/userDataStore/pets?fields=&headless=true&filter=_:startsWith:Pi"));
  }

  @Test
  void testFilter_NotStartsWith_Key() {
    assertJson(
        "[{'key':'bat'},{'key':'cat'},{'key':'cow'},{'key':'crow'},"
            + "{'key':'dog'},{'key':'hamster'},{'key':'horse'},{'key':'snake'}]",
        GET("/userDataStore/pets?fields=&headless=true&filter=_:!startsWith:Pi"));
  }

  @Test
  void testFilter_EndsWith_Key() {
    assertJson(
        "[{'key':'pig'}]", GET("/userDataStore/pets?fields=&headless=true&filter=_:endsWith:iG"));
  }

  @Test
  void testFilter_NotEndsWith_Key() {
    assertJson(
        "[{'key':'bat'},{'key':'cat'},{'key':'cow'},{'key':'crow'},{'key':'dog'},"
            + "{'key':'hamster'},{'key':'horse'},{'key':'pidgin'},{'key':'snake'}]",
        GET("/userDataStore/pets?fields=&headless=true&filter=_:!endsWith:iG"));
  }

  /*
   * Filter combinations
   */

  @Test
  void testFilter_GtAndLt_Key() {
    assertJson(
        "[{'key':'cow'},{'key':'crow'},{'key':'dog'}]",
        GET("/userDataStore/pets?fields=&headless=true&filter=_:gt:cat&filter=_:lt:hamster"));
  }

  @Test
  void testFilter_Or_Key() {
    assertJson(
        "[{'key':'cat'},{'key':'dog'}]",
        GET(
            "/userDataStore/pets?fields=&headless=true&filter=_:eq:cat&filter=_:eq:dog&rootJunction=OR"));
  }

  @Test
  void testFilter_And_Key() {
    assertJson(
        "[{'key':'cat'}]",
        GET(
            "/userDataStore/pets?fields=&headless=true&filter=_:eq:cat&filter=_:like:at&rootJunction=AND"));
  }
}
