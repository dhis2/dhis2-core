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
package org.hisp.dhis.webapi.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * Filters on {@code displayOptions}, a text property stored in a {@code jsonb} column, must return
 * matching objects rather than a database error. Postgres only, as the column type matters.
 *
 * @author Jason P. Pickering <jason@dhis2.org>
 */
@Transactional
class DisplayOptionsFilterPostgresControllerTest extends PostgresControllerIntegrationTestBase {

  private static final String OU = "ouDispOpt01";
  private static final String DATA_SET_A = "dsDispOptA1";
  private static final String DATA_SET_B = "dsDispOptB1";
  private static final String SECTION_A = "seDispOptA1";

  /**
   * A JSON array: jsonb renders it back unchanged, and unlike an object it has no braces, which the
   * test client would read as URI template variables.
   */
  private static final String OPTIONS_A = "[\"alpha\"]";

  @BeforeEach
  void setUp() {
    String metadata =
        """
        {
          "organisationUnits": [
            {"id": "%s", "name": "OU display options", "shortName": "OU DO", "openingDate": "2020-01-01"}
          ],
          "dataSets": [
            {"id": "%s", "name": "DS display A", "shortName": "DS DO A", "periodType": "Monthly",
             "displayOptions": "[\\"alpha\\"]",
             "organisationUnits": [{"id": "%s"}]},
            {"id": "%s", "name": "DS display B", "shortName": "DS DO B", "periodType": "Monthly",
             "displayOptions": "[\\"beta\\"]"}
          ],
          "sections": [
            {"id": "%s", "name": "Section display A", "dataSet": {"id": "%s"},
             "displayOptions": "[\\"alpha\\"]"}
          ]
        }
        """
            .formatted(OU, DATA_SET_A, OU, DATA_SET_B, SECTION_A, DATA_SET_A);
    POST("/metadata", metadata).content(HttpStatus.OK);
  }

  @Test
  void dataSetDisplayOptionsLike() {
    assertEquals(
        List.of(DATA_SET_A), ids("/dataSets?filter=displayOptions:like:alpha", "dataSets"));
  }

  @Test
  void dataSetDisplayOptionsEq() {
    assertEquals(
        List.of(DATA_SET_A), ids("/dataSets?filter=displayOptions:eq:" + OPTIONS_A, "dataSets"));
  }

  @Test
  void dataSetDisplayOptionsIn() {
    assertEquals(
        List.of(DATA_SET_A),
        ids("/dataSets?filter=displayOptions:in:[" + OPTIONS_A + "]", "dataSets"));
  }

  /** A value that is not JSON cannot be bound to the jsonb column, so SQL rejects it. */
  @Test
  void dataSetDisplayOptionsEqPlainText() {
    assertEquals(List.of(), ids("/dataSets?filter=displayOptions:eq:alpha", "dataSets"));
  }

  @Test
  void dataSetDisplayOptionsInPlainText() {
    assertEquals(List.of(), ids("/dataSets?filter=displayOptions:in:[alpha,beta]", "dataSets"));
  }

  @Test
  void sectionDisplayOptionsLike() {
    assertEquals(List.of(SECTION_A), ids("/sections?filter=displayOptions:like:alpha", "sections"));
  }

  private List<String> ids(String url, String listName) {
    return GET(url + "&fields=id")
        .content(HttpStatus.OK)
        .getList(listName, JsonObject.class)
        .stream()
        .map(o -> o.getString("id").string())
        .toList();
  }
}
