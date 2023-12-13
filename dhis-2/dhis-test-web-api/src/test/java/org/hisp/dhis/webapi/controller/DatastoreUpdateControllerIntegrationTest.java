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
package org.hisp.dhis.webapi.controller;

import org.hisp.dhis.webapi.DhisControllerIntegrationTest;

/**
 * Tests the different scenarios for updating a datastore value.
 *
 * <p>Adding keys, updating values and reading them back here need to be run in new transactions so
 * that the changes become visible.
 *
 * @author Jan Bernitt
 */
class DatastoreUpdateControllerIntegrationTest extends DhisControllerIntegrationTest {

  //  @Test
  //  void testUpdateEntry_RootWithNullValue() {
  //    addEntry("ns1", "key1", "42");
  //    updateEntry("/dataStore/ns1/key1");
  //    assertStatus(NOT_FOUND, GET("/dataStore/ns1/key1"));
  //  }
  //
  //  @Test
  //  void testUpdateEntry_PathWithNullValue() {
  //    addEntry("ns2", "key1", "{'a':42}");
  //    updateEntry("/dataStore/ns2/key1?path=a");
  //    assertEquals("{\"a\": null}", GET("/dataStore/ns2/key1").content().node().getDeclaration());
  //  }
  //
  //  @Test
  //  void testUpdateEntry_RootWithNonNullValue() {
  //    addEntry("ns3", "key1", "{'a':42}");
  //    updateEntry("/dataStore/ns3/key1", Body("7"));
  //    assertEquals("7", GET("/dataStore/ns3/key1").content().node().getDeclaration());
  //  }
  //
  //  @Test
  //  void testUpdateEntry_PathWithNonNullValue() {
  //    addEntry("ns4", "key1", "{'a':42}");
  //    updateEntry("/dataStore/ns4/key1?path=a", Body("7"));
  //    assertEquals("{\"a\": 7}", GET("/dataStore/ns4/key1").content().node().getDeclaration());
  //  }
  //
  //  @Test
  //  void testUpdateEntry_RollRootValueIsNull() {
  //    addEntry("ns5", "key1", "null");
  //    updateEntry("/dataStore/ns5/key1?roll=3", Body("7"));
  //    assertEquals("[7]", GET("/dataStore/ns5/key1").content().node().getDeclaration());
  //  }
  //
  //  @Test
  //  void testUpdateEntry_RollRootValueIsArray() {
  //    addEntry("ns6", "key1", "[]");
  //    updateEntry("/dataStore/ns6/key1?roll=3", Body("7"));
  //    assertEquals("[7]", GET("/dataStore/ns6/key1").content().node().getDeclaration());
  //
  //    updateEntry("/dataStore/ns6/key1?roll=3", Body("8"));
  //    doInTransaction(
  //        () -> assertEquals("[7, 8]",
  // GET("/dataStore/ns6/key1").content().node().getDeclaration()));
  //
  //    updateEntry("/dataStore/ns6/key1?roll=3", Body("9"));
  //    doInTransaction(
  //        () ->
  //            assertEquals(
  //                "[7, 8, 9]", GET("/dataStore/ns6/key1").content().node().getDeclaration()));
  //
  //    updateEntry("/dataStore/ns6/key1?roll=3", Body("10"));
  //    doInTransaction(
  //        () ->
  //            assertEquals(
  //                "[8, 9, 10]", GET("/dataStore/ns6/key1").content().node().getDeclaration()));
  //  }
  //
  //  @Test
  //  void testUpdateEntry_RollRootValueIsOther() {
  //    addEntry("ns7", "key1", "{}");
  //    updateEntry("/dataStore/ns7/key1?roll=3", Body("7"));
  //    doInTransaction(
  //        () -> assertEquals("7", GET("/dataStore/ns7/key1").content().node().getDeclaration()));
  //    updateEntry("/dataStore/ns7/key1?roll=3", Body("\"hello\""));
  //    doInTransaction(() -> assertEquals("hello", GET("/dataStore/ns7/key1").content().string()));
  //    updateEntry("/dataStore/ns7/key1?roll=3", Body("true"));
  //    doInTransaction(() -> assertTrue(GET("/dataStore/ns7/key1").content().booleanValue()));
  //  }
  //
  //  @Test
  //  void testUpdateEntry_RollPathValueIsNull() {
  //    addEntry("ns8", "key1", "{'a':null}");
  //    updateEntry("/dataStore/ns8/key1?roll=3&path=a", Body("7"));
  //    assertEquals("{\"a\": [7]}", GET("/dataStore/ns8/key1").content().node().getDeclaration());
  //  }
  //
  //  @Test
  //  void testUpdateEntry_RollPathValueIsUndefined() {
  //    addEntry("ns9", "key1", "{'a':null}");
  //    updateEntry("/dataStore/ns9/key1?roll=3&path=b", Body("7"));
  //    assertEquals(
  //        "{\"a\": null, \"b\": [7]}",
  // GET("/dataStore/ns9/key1").content().node().getDeclaration());
  //  }
  //
  //  @Test
  //  void testUpdateEntry_RollPathValueIsArray() {
  //    addEntry("ns10", "key1", "{'a':{'b':[]}}");
  //    updateEntry("/dataStore/ns10/key1?roll=3&path=a.b", Body("7"));
  //    assertEquals("[7]",
  // GET("/dataStore/ns10/key1").content().get("a.b").node().getDeclaration());
  //
  //    updateEntry("/dataStore/ns10/key1?roll=3&path=a.b", Body("8"));
  //    doInTransaction(
  //        () ->
  //            assertEquals(
  //                "[7, 8]",
  //                GET("/dataStore/ns10/key1").content().get("a.b").node().getDeclaration()));
  //
  //    updateEntry("/dataStore/ns10/key1?roll=3&path=a.b", Body("9"));
  //    doInTransaction(
  //        () ->
  //            assertEquals(
  //                "[7, 8, 9]",
  //                GET("/dataStore/ns10/key1").content().get("a.b").node().getDeclaration()));
  //
  //    updateEntry("/dataStore/ns10/key1?roll=3&path=a.b", Body("10"));
  //    doInTransaction(
  //        () ->
  //            assertEquals(
  //                "[8, 9, 10]",
  //                GET("/dataStore/ns10/key1").content().get("a.b").node().getDeclaration()));
  //  }
  //
  //  @Test
  //  void testUpdateEntry_RollPathValueIsOther() {
  //    addEntry("ns11", "key1", "{'a':[{}]}");
  //    updateEntry("/dataStore/ns11/key1?roll=3&path=a.[0]", Body("7"));
  //    doInTransaction(
  //        () ->
  //            assertEquals(
  //                "{\"a\": [7]}", GET("/dataStore/ns11/key1").content().node().getDeclaration()));
  //
  //    updateEntry("/dataStore/ns11/key1?roll=3&path=a.[0]", Body("\"hello\""));
  //    doInTransaction(
  //        () ->
  //            assertEquals(
  //                "{\"a\": [\"hello\"]}",
  //                GET("/dataStore/ns11/key1").content().node().getDeclaration()));
  //
  //    updateEntry("/dataStore/ns11/key1?roll=3&path=a.[0]", Body("true"));
  //    doInTransaction(
  //        () ->
  //            assertEquals(
  //                "{\"a\": [true]}",
  // GET("/dataStore/ns11/key1").content().node().getDeclaration()));
  //  }
  //
  //  void updateEntry(String url, Object... args) {
  //    doInTransaction(() -> assertStatus(OK, PUT(url, args)));
  //  }
  //
  //  /**
  //   * The reason this is needed in this test is that we need the creation and update run in
  //   * transactions that are closed as they use different technology stacks. Without this the
  // update
  //   * does not become visible when reading back the value, and it appears as if the value is
  // still
  //   * the value from creation.
  //   */
  //  private void addEntry(String ns, String key, String value) {
  //    doInTransaction(
  //        () -> assertStatus(HttpStatus.CREATED, PUT("/dataStore/" + ns + "/" + key, value)));
  //  }
}
