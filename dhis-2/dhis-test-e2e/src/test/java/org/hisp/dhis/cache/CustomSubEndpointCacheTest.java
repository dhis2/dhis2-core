/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.test.e2e.Constants;
import org.hisp.dhis.test.e2e.actions.RestApiActions;
import org.hisp.dhis.test.e2e.utils.DataGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Custom sub-endpoints below a metadata resource read or write data the resource's smart ETag does
 * not track. They must get no smart ETag, so a revalidating client always reaches the controller.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class CustomSubEndpointCacheTest extends CacheApiTest {

  @BeforeEach
  void loginAsSuperUser() {
    loginActions.loginAsSuperUser();
  }

  @ParameterizedTest
  @ValueSource(strings = {"generate", "generateAndReserve"})
  void generateReservesANewValueOnRevalidation(String endpoint) {
    String attribute =
        new RestApiActions("/trackedEntityAttributes")
            .create(
                """
                {"name": "Cache generated %1$s", "shortName": "Cache gen %1$s",
                 "valueType": "TEXT", "aggregationType": "NONE", "unique": true,
                 "generated": true, "pattern": "RANDOM(#######)"}
                """
                    .formatted(DataGenerator.randomString()));
    String path = "/trackedEntityAttributes/" + attribute + "/" + endpoint;

    CacheProbe.CacheResponse first = getWithoutSmartETag(path);

    CacheProbe.CacheResponse second = probe.getIfNoneMatch(path, first.etag());

    assertEquals(200, second.statusCode(), "a revalidation must reserve a new value");
    assertNotEquals(first.body(), second.body());
  }

  @Test
  void sqlViewDataFollowsTheRowsItReads() {
    String sqlView =
        new RestApiActions("/sqlViews")
            .create(
                """
                {"name": "Cache constants %s", "type": "QUERY", "cacheStrategy": "NO_CACHE",
                 "sqlQuery": "select count(*) as constants from constant"}
                """
                    .formatted(DataGenerator.randomString()));
    String path = "/sqlViews/" + sqlView + "/data";

    CacheProbe.CacheResponse first = getWithoutSmartETag(path);

    createConstant();
    CacheProbe.CacheResponse second = probe.getIfNoneMatch(path, first.etag());

    assertEquals(200, second.statusCode(), "the view reads a new row");
    assertNotEquals(first.body(), second.body());
  }

  @Test
  void dataSetFormFollowsItsDataElements() {
    String name = "Cache form element " + DataGenerator.randomString();
    RestApiActions dataElements = new RestApiActions("/dataElements");
    String dataElement = dataElements.create(dataElementJson(name));
    String dataSet =
        new RestApiActions("/dataSets")
            .create(
                """
                {"name": "Cache form %1$s", "shortName": "Cache form %1$s",
                 "periodType": "Monthly", "dataSetElements": [{"dataElement": {"id": "%2$s"}}]}
                """
                    .formatted(DataGenerator.randomString(), dataElement));
    String path = "/dataSets/" + dataSet + "/form?ou=" + Constants.ORG_UNIT_IDS[0];

    CacheProbe.CacheResponse first = getWithoutSmartETag(path);

    String renamed = name + " renamed";
    dataElements.patch(dataElement, "replace", "/name", renamed).validateStatus(200);
    CacheProbe.CacheResponse second = probe.getIfNoneMatch(path, first.etag());

    assertEquals(200, second.statusCode(), "the form shows the renamed data element");
    assertTrue(second.body().contains(renamed), second.body());
  }

  private CacheProbe.CacheResponse getWithoutSmartETag(String path) {
    CacheProbe.CacheResponse response = probe.get(path);
    assertEquals(200, response.statusCode(), response.body());
    CacheAssertions.assertNoSmartCacheHeaders(response);
    return response;
  }

  private static String dataElementJson(String name) {
    return """
        {"name": "%1$s", "shortName": "%1$s", "valueType": "NUMBER",
         "domainType": "AGGREGATE", "aggregationType": "SUM"}
        """
        .formatted(name);
  }

  private static void createConstant() {
    new RestApiActions("/constants")
        .create(
            """
            {"name": "Cache constant %1$s", "shortName": "Cache constant %1$s", "value": 1}
            """
                .formatted(DataGenerator.randomString()));
  }
}
