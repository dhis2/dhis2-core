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
package org.hisp.dhis.webapi.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonNodeType;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link org.hisp.dhis.gist.GistPager} related features of the Gist API.
 *
 * @author Jan Bernitt
 */
class GistPagerControllerTest extends AbstractGistControllerTest {
  @Test
  void testPager_Total_ResultBased() {
    String baseUrl = "/users/{uid}/userGroups/gist?fields=name,users";
    JsonObject gist = GET(baseUrl + "&total=true", getAdminUid()).content();
    assertHasPager(gist, 1, 50, 1);
    gist = GET(baseUrl + "&totalPages=true", getAdminUid()).content();
    assertHasPager(gist, 1, 50, 1);

    JsonWebMessage msg =
        GET(baseUrl + "&totalPages=true&total=false", getAdminUid())
            .content(HttpStatus.BAD_REQUEST)
            .as(JsonWebMessage.class);
    assertEquals(
        "totalPages and total request parameters are contradicting each other", msg.getMessage());
  }

  @Test
  void testPager_CustomPageListName() {
    JsonObject gist =
        GET(
                "/users/{uid}/userGroups/gist?fields=name,users&total=true&pageListName=matches",
                getAdminUid())
            .content();
    JsonArray matches = gist.getArray("matches");
    assertTrue(matches.exists());
    assertTrue(matches.isArray());
  }

  @Test
  void testPager_Total_CountQueryNonExistingPage() {
    JsonObject gist =
        GET("/users/{uid}/userGroups/gist?fields=name,users&total=true&page=6", getAdminUid())
            .content();
    assertHasPager(gist, 6, 50, 1);
  }

  @Test
  void testPager_Total_CountQuery() {
    // create some members we can count a total for
    createDataSetsForOrganisationUnit(10, orgUnitId, "extra");
    String url =
        "/organisationUnits/{id}/dataSets/gist?total=true&pageSize=3&order=name&filter=name:startsWith:extra";
    JsonObject gist = GET(url, orgUnitId).content();
    assertHasPager(gist, 1, 3, 10);
    // now page 2
    gist = GET(url + "&page=2", orgUnitId).content();
    assertHasPager(gist, 2, 3, 10);
    assertEquals(
        "/organisationUnits/{id}/dataSets/gist?total=true&pageSize=3&order=name&filter=name:startsWith:extra&page=1"
            .replace("{id}", orgUnitId),
        gist.getObject("pager").getString("prevPage").string());
    assertEquals(
        "/organisationUnits/{id}/dataSets/gist?total=true&pageSize=3&order=name&filter=name:startsWith:extra&page=3"
            .replace("{id}", orgUnitId),
        gist.getObject("pager").getString("nextPage").string());
    JsonArray dataSets = gist.getArray("dataSets");
    assertEquals("extra3", dataSets.getObject(0).getString("name").string());
    assertEquals("extra4", dataSets.getObject(1).getString("name").string());
    assertEquals("extra5", dataSets.getObject(2).getString("name").string());
  }

  @Test
  void testHeadless() {
    String baseUrl = "/users/{uid}/userGroups/gist";
    assertEquals(
        JsonNodeType.OBJECT,
        GET(baseUrl, getAdminUid()).content().node().getType(),
        "by default list should have a pager");

    assertEquals(
        JsonNodeType.ARRAY,
        GET(baseUrl + "?headless=true", getAdminUid()).content().node().getType());
    assertEquals(
        JsonNodeType.OBJECT,
        GET(baseUrl + "?headless=false", getAdminUid()).content().node().getType());
  }
}
