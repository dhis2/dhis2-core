/*
 * Copyright (c) 2004-2022, University of Oslo
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

import static org.hisp.dhis.utils.JavaToJson.toJson;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * Tests specifically the {@code fields} parameter aspect of the {@link
 * DatastoreController#getEntries(String, String, boolean, HttpServletRequest, HttpServletResponse)}
 * method using (mocked) REST requests.
 *
 * <p>Tests will use {@code filter} but only to pick keys in order to reduce the matches.
 *
 * @author Jan Bernitt
 */
class DatastoreFieldsControllerTest extends AbstractDatastoreControllerTest {
  @BeforeEach
  void setUp() {
    // simple values
    postEntry("pets", "dog", toJson(false));
    postEntry("pets", "bat", toJson(true));
    postEntry("pets", "pidgin", toJson(42));
    postEntry("pets", "horse", toJson("Fury"));
    postEntry("pets", "snake", toJson((Object) null));

    // objects
    postPet("cat", "Miao", 9, List.of("tuna", "mice", "birds"));
    postPet("cow", "Muuhh", 5, List.of("gras"));
    postPet("hamster", "Speedy", 2, List.of("veggies"));
    postPet("pig", "Oink", 6, List.of("carrots", "potatoes"));
  }

  @Test
  void testFields_KeyOnly() {
    assertJson("[{'key':'cat'}]", GET("/dataStore/pets?fields=&headless=true&filter=_:eq:cat"));
  }

  @Test
  void testFields_RootBoolean() {
    assertJson(
        "[{'key':'dog','value':false}]",
        GET("/dataStore/pets?fields=.&headless=true&filter=_:eq:dog"));
  }

  @Test
  void testFields_RootNull() {
    assertJson(
        "[{'key':'snake','value':null}]",
        GET("/dataStore/pets?fields=.&headless=true&filter=_:eq:snake"));
  }

  @Test
  void testFields_RootNumber() {
    assertJson(
        "[{'key':'pidgin','value':42}]",
        GET("/dataStore/pets?fields=.&headless=true&filter=_:eq:pidgin"));
  }

  @Test
  void testFields_RootString() {
    assertJson(
        "[{'key':'horse','value':'Fury'}]",
        GET("/dataStore/pets?fields=.&headless=true&filter=_:eq:horse"));
  }

  @Test
  void testFields_RootAlias() {
    assertJson(
        "[{'key':'horse','name':'Fury'}]",
        GET("/dataStore/pets?fields=.~hoist(name)&headless=true&filter=_:eq:horse"));
  }

  @Test
  void testFields_PathBoolean() {
    assertJson(
        "[{'key':'hamster','cute':true}]",
        GET("/dataStore/pets?fields=cute&headless=true&filter=_:eq:hamster"));
  }

  @Test
  void testFields_PathNumber() {
    assertJson(
        "[{'key':'cat','age':9}]", GET("/dataStore/pets?fields=age&headless=true&filter=_:eq:cat"));
  }

  @Test
  void testFields_PathString() {
    assertJson(
        "[{'key':'cat','name':'Miao'},"
            + "{'key':'cow','name':'Muuhh'},"
            + "{'key':'hamster','name':'Speedy'},"
            + "{'key':'pig','name':'Oink'}]",
        GET("/dataStore/pets?fields=name&headless=true"));
  }

  @Test
  void testFields_PathStringWithAlias() {
    String expected =
        "["
            + "{'key':'cat','food':{'name':'tuna'}},"
            + "{'key':'cow','food':{'name':'gras'}},"
            + "{'key':'hamster','food':{'name':'veggies'}},"
            + "{'key':'pig','food':{'name':'carrots'}}"
            + "]";
    assertJson(expected, GET("/dataStore/pets?fields=eats.0~hoist(food)&headless=true"));
    assertJson(expected, GET("/dataStore/pets?fields=eats[0~hoist(food)]&headless=true"));
    assertJson(expected, GET("/dataStore/pets?fields=eats(0~hoist(food))&headless=true"));
  }

  @Test
  void testFields_Multiple() {
    String expected =
        "["
            + "{'key':'cat','name':'Miao','food':'tuna'},"
            + "{'key':'cow','name':'Muuhh','food':'gras'},"
            + "{'key':'hamster','name':'Speedy','food':'veggies'},"
            + "{'key':'pig','name':'Oink','food':'carrots'}"
            + "]";
    assertJson(expected, GET("/dataStore/pets?fields=name,eats.0.name~hoist(food)&headless=true"));
    assertJson(
        expected, GET("/dataStore/pets?fields=name,eats[0[name~hoist(food)]]&headless=true"));
    assertJson(
        expected, GET("/dataStore/pets?fields=name,eats(0(name~hoist(food)))&headless=true"));
  }

  @Test
  void testGetEntries_IncludeAllTrue() {
    assertJson(
        "[" + "{'key':'cat','name':'Miao'}," + "{'key':'dog','name':null}" + "]",
        GET("/dataStore/pets?fields=name&includeAll=true&headless=true&filter=_:in:[cat,dog]"));
  }

  @Test
  void testGetEntries_IllegalPath() {
    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        "Illegal fields expression. Expected `,`, `[` or `]` at position 7 but found `'`",
        GET("/dataStore/pets?fields=illegal'&headless=true").content(HttpStatus.CONFLICT));
  }
}
