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

import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonIdentifiableObject;
import org.hisp.dhis.webapi.json.domain.JsonOptionSet;
import org.junit.jupiter.api.Test;

class OptionControllerTest extends DhisControllerConvenienceTest {
  @Test
  void testUpdateOptionWithSortOrderGap() {
    // Create OptionSet with two Options
    POST(
            "/metadata",
            "{\"optionSets\":\n"
                + "    [{\"name\": \"Device category\",\"id\": \"RHqFlB1Wm4d\",\"version\": 2,\"valueType\": \"TEXT\",\"options\":[{\"id\": \"Uh4HvjK6zg3\"},{\"id\": \"BQMei56UBl6\"}]}],\n"
                + "\"options\":\n"
                + "    [{\"code\": \"Vaccine freezer\",\"name\": \"Vaccine freezer\",\"id\": \"BQMei56UBl6\",\"sortOrder\": 1,\"optionSet\":{\"id\": \"RHqFlB1Wm4d\"}},\n"
                + "    {\"code\": \"Icelined refrigerator\",\"name\": \"Icelined refrigerator\",\"id\": \"Uh4HvjK6zg3\",\"sortOrder\": 2,\"optionSet\":{\"id\": \"RHqFlB1Wm4d\"}}]}")
        .content(HttpStatus.OK);

    JsonObject response =
        GET("/optionSets/{uid}?fields=options[id,sortOrder]", "RHqFlB1Wm4d").content();

    // sortOrder is 1 and 2
    assertEquals(2, response.getObject("options").size());
    assertEquals(1, response.getNumber("options[0].sortOrder").intValue());
    assertEquals(2, response.getNumber("options[1].sortOrder").intValue());

    // Update option sortOrder 2 to 20
    POST(
            "/metadata",
            "{\"options\":\n"
                + "[{\"code\": \"Icelined refrigerator\",\"name\": \"Icelined refrigerator\",\"id\": \"Uh4HvjK6zg3\",\"sortOrder\": 20,\"optionSet\":{\"id\": \"RHqFlB1Wm4d\"}}]}")
        .content(HttpStatus.OK);

    response = GET("/optionSets/{uid}?fields=options[id,sortOrder]", "RHqFlB1Wm4d").content();
    assertEquals(2, response.getObject("options").size());
    assertEquals(1, response.getNumber("options[0].sortOrder").intValue());
    assertEquals("Uh4HvjK6zg3", response.getString("options[1].id").string());
    assertEquals(20, response.getNumber("options[1].sortOrder").intValue());
  }

  @Test
  void testImportOptionWithoutSortOrder() {
    String id =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/optionSets/",
                "{'name': 'test', 'version': 2, 'valueType': 'TEXT', 'description':'desc' }"));
    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/options/",
            "{'optionSet': { 'id':'"
                + id
                + "'}, 'id':'Uh4HvjK6zg3', 'code': 'A', 'name': 'Anna', 'description': 'this-is-a'}"));
    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/options/",
            "{'optionSet': { 'id':'"
                + id
                + "'},'id':'BQMei56UBl6','code': 'B', 'name': 'Betta', 'description': 'this-is-b'}"));
    JsonOptionSet set =
        GET("/optionSets/{id}?fields=options[id,sortOrder]", id).content().as(JsonOptionSet.class);
    assertEquals("Uh4HvjK6zg3", set.getOptions().get(0).getId());
    assertEquals(0, set.getOptions().get(0).getSortOrder());
    assertEquals("BQMei56UBl6", set.getOptions().get(1).getId());
    assertEquals(0, set.getOptions().get(1).getSortOrder());
  }

  @Test
  void testOptionSetsWithDescription() {
    String id =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/optionSets/",
                "{'name': 'test', 'version': 2, 'valueType': 'TEXT', 'description':'desc' }"));
    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/options/",
            "{'optionSet': { 'id':'"
                + id
                + "'}, 'code': 'A', 'name': 'Anna', 'description': 'this-is-a', 'sortOrder': 1}"));
    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/options/",
            "{'optionSet': { 'id':'"
                + id
                + "'},'code': 'B', 'name': 'Betta', 'description': 'this-is-b', 'sortOrder': 2}"));

    JsonOptionSet set =
        GET("/optionSets/{id}?fields=id,name,description,options[id,name,description]", id)
            .content()
            .as(JsonOptionSet.class);
    assertEquals("desc", set.getDescription());
    assertEquals(
        List.of("this-is-a", "this-is-b"),
        set.getOptions().toList(JsonIdentifiableObject::getDescription));
  }
}
