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
package org.hisp.dhis.webapi.json.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.LocalDateTime;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonMap;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonResponse;
import org.hisp.dhis.jsontree.JsonTypedAccess;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link JsonResponse} with domain specific cases.
 *
 * @author Jan Bernitt
 */
class DomainJsonResponseTest {
  @Test
  void testCustomObjectType() {
    JsonObject response = createJSON("{'user': {'id':'foo'}}");
    assertEquals("foo", response.get("user", JsonUser.class).getId());
  }

  @Test
  void testCustomObjectTypeList() {
    JsonObject response = createJSON("{'users': [ {'id':'foo'} ]}");
    JsonList<JsonUser> users = response.getList("users", JsonUser.class);
    assertEquals("foo", users.get(0).getId());
  }

  @Test
  void testCustomObjectTypeMap() {
    JsonObject response = createJSON("{'users': {'foo':{'id':'foo'}, 'bar':{'id':'bar'}}}");
    JsonMap<JsonUser> usersById = response.getMap("users", JsonUser.class);
    assertFalse(usersById.isEmpty());
    assertEquals(2, usersById.size());
    assertEquals("foo", usersById.get("foo").getId());
  }

  @Test
  void testDateType() {
    JsonObject response = createJSON("{'user': {'lastUpdated': '2021-01-21T15:14:54.000'}}");
    JsonUser user = response.get("user", JsonUser.class);
    assertEquals(LocalDateTime.of(2021, 1, 21, 15, 14, 54), user.getLastUpdated());
    Assertions.assertNull(user.getCreated());
  }

  @Test
  void testErrorSummary_MessageOnly() {
    JsonObject response =
        createJSON(
            "{"
                + "'message':'my message',"
                + "'httpStatus':'CONFLICT',"
                + "'httpStatusCode':409,"
                + "}");
    assertEquals("my message", response.as(JsonError.class).summary());
  }

  @Test
  void testErrorSummary_MessageAndErrorReports() {
    JsonObject response =
        createJSON(
            "{"
                + "'message':'my message',"
                + "'httpStatus':'CONFLICT',"
                + "'httpStatusCode':409,"
                + "'response':{'errorReports': [{'errorCode':'E4000','message':'m1'}]}"
                + "}");
    assertEquals("my message\n" + "  E4000 m1", response.as(JsonError.class).summary());
  }

  @Test
  void testErrorSummary_MessageAndObjectReports() {
    JsonObject response =
        createJSON(
            "{"
                + "'message':'my message',"
                + "'httpStatus':'CONFLICT',"
                + "'httpStatusCode':409,"
                + "'response':{'objectReports':[{'klass':'java.lang.String','errorReports': [{'errorCode':'E4000','message':'m1'}]}]}"
                + "}");
    assertEquals(
        "my message\n" + "* class java.lang.String\n" + "  E4000 m1",
        response.as(JsonError.class).summary());
  }

  private JsonResponse createJSON(String content) {
    return new JsonResponse(content.replace('\'', '"'), JsonTypedAccess.GLOBAL);
  }
}
